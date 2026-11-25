package com.vybe.music.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface MusicApiService {

    // ── Primary Global Catalog: iTunes Search API ────────────────────────────
    @GET("https://itunes.apple.com/search")
    suspend fun searchITunes(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25,
        @Query("country") country: String? = "IN"
    ): ITunesSearchResponse

    @GET("https://itunes.apple.com/search")
    suspend fun searchITunesGlobal(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25
    ): ITunesSearchResponse

    // ── Direct Official Studio Audio Stream Search (for DES Decryption) ──────
    @GET("https://www.jiosaavn.com/api.php")
    suspend fun searchDirectSaavn(
        @Query("__call") call: String = "search.getResults",
        @Query("_format") format: String = "json",
        @Query("_marker") marker: String = "0",
        @Query("ctx") ctx: String = "android",
        @Query("p") page: Int = 1,
        @Query("n") limit: Int = 5,
        @Query("q") query: String
    ): JioSaavnDirectSearchResponse

    // ── Legacy Compatibility Endpoints ──────────────────────────────────────
    @GET
    suspend fun searchITunesPoster(
        @Url fullUrl: String,
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 1
    ): ITunesPosterResponse

    @GET
    suspend fun searchSongsOnMirror(
        @Url fullUrl: String,
        @Query("query") query: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): JioSaavnSearchResponse

    companion object {
        const val DEFAULT_BASE_URL = "https://itunes.apple.com/"

        val instance: MusicApiService by lazy {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            Retrofit.Builder()
                .baseUrl(DEFAULT_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MusicApiService::class.java)
        }
    }
}
