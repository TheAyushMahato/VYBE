package com.vybe.music.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.vybe.music.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val apkUrl: String = "",
    val changelog: String = "",
    val forceUpdate: Boolean = false
)

enum class UpdateDownloadState {
    IDLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    FAILED
}

class AppUpdateManager(private val context: Context) {
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloadState = MutableStateFlow(UpdateDownloadState.IDLE)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isDismissed = MutableStateFlow(false)
    val isDismissed: StateFlow<Boolean> = _isDismissed.asStateFlow()

    suspend fun checkForUpdates(): AppUpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("app_config").document("update").get().await()
                if (snapshot.exists()) {
                    val latestVersionCode = snapshot.getLong("latestVersionCode") ?: 0L
                    val latestVersionName = snapshot.getString("latestVersionName").orEmpty()
                    val apkUrl = snapshot.getString("apkUrl").orEmpty()
                    val changelog = snapshot.getString("changelog").orEmpty()
                    val forceUpdate = snapshot.getBoolean("forceUpdate") ?: false

                    val currentCode = BuildConfig.VERSION_CODE.toLong()
                    val currentVersionName = BuildConfig.VERSION_NAME
                    Log.d("AppUpdateManager", "Current versionCode: $currentCode, Firestore latest: $latestVersionCode, remoteName: $latestVersionName")

                    val isNewer = isRemoteVersionNewer(
                        remoteName = latestVersionName,
                        remoteCode = latestVersionCode,
                        currentName = currentVersionName,
                        currentCode = currentCode
                    )

                    if (isNewer && apkUrl.isNotBlank()) {
                        val info = AppUpdateInfo(
                            latestVersionCode = latestVersionCode,
                            latestVersionName = latestVersionName,
                            apkUrl = apkUrl,
                            changelog = changelog,
                            forceUpdate = forceUpdate
                        )
                        _updateInfo.value = info
                        info
                    } else {
                        _updateInfo.value = null
                        null
                    }
                } else {
                    _updateInfo.value = null
                    null
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Failed to check for updates", e)
                null
            }
        }
    }

    suspend fun startApkDownloadAndInstall(context: Context) {
        val info = _updateInfo.value ?: return
        val apkUrl = info.apkUrl
        if (apkUrl.isBlank()) return

        withContext(Dispatchers.IO) {
            try {
                _downloadState.value = UpdateDownloadState.DOWNLOADING
                _downloadProgress.value = 0f

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val request = Request.Builder()
                    .url(apkUrl)
                    .header("User-Agent", "VYBE-InAppUpdate")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e("AppUpdateManager", "Download failed with HTTP ${response.code}")
                    _downloadState.value = UpdateDownloadState.FAILED
                    return@withContext
                }

                val body = response.body ?: run {
                    _downloadState.value = UpdateDownloadState.FAILED
                    return@withContext
                }

                val totalBytes = body.contentLength()
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val apkFile = File(cacheDir, "vybe-update.apk")
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        var lastProgressUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val now = System.currentTimeMillis()
                            if (totalBytes > 0 && now - lastProgressUpdate > 120) {
                                _downloadProgress.value = (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                lastProgressUpdate = now
                            }
                        }
                        output.flush()
                    }
                }

                _downloadProgress.value = 1f
                _downloadState.value = UpdateDownloadState.READY_TO_INSTALL

                withContext(Dispatchers.Main) {
                    launchPackageInstaller(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e("AppUpdateManager", "Error downloading APK update", e)
                _downloadState.value = UpdateDownloadState.FAILED
            }
        }
    }

    fun launchPackageInstaller(context: Context, apkFile: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("AppUpdateManager", "Failed to launch installer intent", e)
        }
    }

    fun dismissUpdate() {
        _isDismissed.value = true
    }

    companion object {
        fun isRemoteVersionNewer(
            remoteName: String,
            remoteCode: Long,
            currentName: String,
            currentCode: Long
        ): Boolean {
            if (remoteCode <= currentCode) return false
            fun parseParts(name: String): List<Int> {
                return name.trim().removePrefix("v").split(".")
                    .mapNotNull { it.takeWhile { ch -> ch.isDigit() }.toIntOrNull() }
            }
            val remoteParts = parseParts(remoteName)
            val currentParts = parseParts(currentName)
            if (remoteParts.isNotEmpty() && currentParts.isNotEmpty()) {
                val maxLen = maxOf(remoteParts.size, currentParts.size)
                for (i in 0 until maxLen) {
                    val r = remoteParts.getOrElse(i) { 0 }
                    val c = currentParts.getOrElse(i) { 0 }
                    if (r > c) return true
                    if (r < c) return false
                }
                return false
            }
            return remoteCode > currentCode
        }
    }
}
