package com.vybe.music.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Centralized User Session Manager:
 * Tracks the active user ID (Firebase Auth UID or sanitized email hash, falling back to "guest").
 * Provides user-scoped SharedPreferences naming and user-scoped file directories so every Google
 * account has completely isolated Liked Songs, Playlists, Downloads, Scrobbles, and Cache.
 */
object UserSessionManager {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    private val _currentUserId = MutableStateFlow(computeUserId(auth.currentUser))
    val currentUserIdFlow: StateFlow<String> = _currentUserId.asStateFlow()

    val currentUserId: String
        get() = _currentUserId.value

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val newId = computeUserId(firebaseAuth.currentUser)
            if (_currentUserId.value != newId) {
                _currentUserId.value = newId
            }
        }
    }

    fun computeUserId(user: FirebaseUser?): String {
        val raw = user?.uid ?: return "guest"
        val sanitized = raw.replace(Regex("[^a-zA-Z0-9_-]"), "_").trim()
        return if (sanitized.isNotBlank()) sanitized else "guest"
    }

    /**
     * Returns a user-scoped SharedPreferences file name, e.g. "vybe_local_storage_u12345"
     */
    fun getUserPrefsName(prefix: String, userId: String = currentUserId): String {
        return "${prefix}_${userId}"
    }

    /**
     * Returns a user-scoped downloads directory, e.g. "files/downloads/u12345/"
     */
    fun getUserDownloadsDir(context: Context, userId: String = currentUserId): File {
        val dir = File(context.filesDir, "downloads/$userId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * One-time seamless migration of legacy un-scoped preferences and download files
     * into the active user's scoped preferences/directories if target is empty.
     * Clears legacy preferences after copying to guarantee future new accounts start 100% clean.
     */
    fun migrateLegacyPrefsIfNeeded(context: Context, targetUserId: String) {
        if (targetUserId == "guest") return
        val legacyPrefs = context.getSharedPreferences("vybe_local_storage", Context.MODE_PRIVATE)
        val targetPrefsName = getUserPrefsName("vybe_local_storage", targetUserId)
        val targetPrefs = context.getSharedPreferences(targetPrefsName, Context.MODE_PRIVATE)

        if (targetPrefs.all.isEmpty() && legacyPrefs.all.isNotEmpty()) {
            val editor = targetPrefs.edit()
            for ((key, value) in legacyPrefs.all) {
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                }
            }
            editor.apply()
            // Clear legacy prefs so subsequent accounts never accidentally inherit them
            legacyPrefs.edit().clear().apply()

            // Also move legacy downloaded files to the target user's download directory
            val legacyDownloadsDir = File(context.filesDir, "downloads")
            val targetDir = getUserDownloadsDir(context, targetUserId)
            if (legacyDownloadsDir.exists() && legacyDownloadsDir.isDirectory) {
                legacyDownloadsDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val dest = File(targetDir, file.name)
                        if (!dest.exists()) {
                            file.renameTo(dest)
                        }
                    }
                }
            }
        }
    }
}
