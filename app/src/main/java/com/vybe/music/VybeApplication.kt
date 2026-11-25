package com.vybe.music

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class VybeApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")

                var response = try {
                    chain.proceed(requestBuilder.build())
                } catch (e: Exception) {
                    null
                }

                if (response != null && response.isSuccessful) {
                    response
                } else {
                    response?.close()
                    val urlString = original.url.toString()
                    val fallbackUrl = when {
                        urlString.contains("maxresdefault.jpg") -> {
                            urlString.replace("maxresdefault.jpg", "hqdefault.jpg")
                        }
                        urlString.contains("sddefault.jpg") -> {
                            urlString.replace("sddefault.jpg", "hqdefault.jpg")
                        }
                        urlString.contains("mqdefault.jpg") -> {
                            urlString.replace("mqdefault.jpg", "hqdefault.jpg")
                        }
                        urlString.contains("=w544-h544") || urlString.contains("=w") -> {
                            urlString.replace(Regex("=w\\d+-h\\d+.*"), "=w500-h500")
                        }
                        else -> null
                    }

                    if (!fallbackUrl.isNullOrBlank() && fallbackUrl != urlString) {
                        val fallbackRequest = original.newBuilder()
                            .url(fallbackUrl)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                            .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .build()
                        try {
                            chain.proceed(fallbackRequest)
                        } catch (e: Exception) {
                            chain.proceed(requestBuilder.build())
                        }
                    } else {
                        try {
                            chain.proceed(requestBuilder.build())
                        } catch (e: Exception) {
                            throw e
                        }
                    }
                }
            }
            .build()

        val builder = ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .respectCacheHeaders(false)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(180)

        try {
            builder.diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150MB
                    .build()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val loader = builder.build()
        coil.Coil.setImageLoader(loader)
        return loader
    }
}
