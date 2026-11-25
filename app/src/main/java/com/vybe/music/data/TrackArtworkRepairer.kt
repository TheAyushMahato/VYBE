package com.vybe.music.data

import android.content.Context
import com.vybe.music.model.Track
import kotlinx.coroutines.flow.SharedFlow

/**
 * Backward-compatible wrapper delegating to [ArtworkRepairManager].
 */
object TrackArtworkRepairer {
    val artworkRepairedFlow: SharedFlow<Pair<String, String>>
        get() = ArtworkRepairManager.artworkRepairedFlow

    fun repairArtworkIfNeeded(
        context: Context,
        track: Track,
        onRepaired: ((String) -> Unit)? = null
    ) {
        ArtworkRepairManager.repairIfNeeded(context, track, onRepaired)
    }

    suspend fun resolveRepairedArtwork(context: Context, track: Track): String? {
        val storage = LocalMusicStorage(context)
        val repaired = ArtworkRepairManager.getRepairedUrl(track)
        if (!repaired.isNullOrBlank()) return repaired
        ArtworkRepairManager.repairIfNeeded(context, track)
        return ArtworkRepairManager.getRepairedUrl(track) ?: track.artworkUrl.ifBlank { null }
    }
}
