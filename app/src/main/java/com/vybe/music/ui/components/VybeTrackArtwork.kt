package com.vybe.music.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.vybe.music.data.ArtworkRepairManager
import com.vybe.music.model.Track

/**
 * Universal Track Artwork component with reactive auto-repair and shimmer loading.
 *
 * Resolves artwork in priority order:
 * 1. Valid local cover file (> 2KB, validated image decode bounds)
 * 2. In-memory Compose-observable ArtworkRepairManager.repairedUrls
 * 3. track.artworkUrl (auto-upgrading http:// to https://)
 *
 * If artwork is missing, blank, or fails with 404/403, triggers ArtworkRepairManager
 * in the background while displaying a smooth AMOLED skeleton shimmer.
 */
@Composable
fun VybeTrackArtwork(
    track: Track,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentDescription: String? = track.title,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    val validLocalCover = remember(track.id) {
        ArtworkRepairManager.getValidLocalCover(context, track.id)
    }

    // Reactive Compose observation from SnapshotStateMap
    val repairedMemoryUrl = ArtworkRepairManager.getRepairedUrl(track)

    val effectiveModel: Any? = remember(track.id, track.artworkUrl, validLocalCover, repairedMemoryUrl) {
        when {
            validLocalCover != null -> Uri.fromFile(validLocalCover)
            !repairedMemoryUrl.isNullOrBlank() -> repairedMemoryUrl
            track.artworkUrl.isNotBlank() -> {
                var url = track.artworkUrl.trim()
                if (url.startsWith("http://")) {
                    url = "https://" + url.removePrefix("http://")
                }
                url
            }
            track.id.length == 11 && track.id.matches(Regex("^[a-zA-Z0-9_-]{11}$")) -> {
                "https://i.ytimg.com/vi/${track.id}/hqdefault.jpg"
            }
            else -> null
        }
    }

    // Trigger repair if model is null/blank
    LaunchedEffect(track.id, effectiveModel) {
        if (effectiveModel == null || (effectiveModel is String && effectiveModel.isBlank())) {
            ArtworkRepairManager.repairIfNeeded(context, track)
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF1E1E22)),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(effectiveModel)
                .crossfade(180)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        ) {
            val state = painter.state
            when (state) {
                is AsyncImagePainter.State.Loading -> {
                    VybeSkeletonBox(modifier = Modifier.fillMaxSize(), shape = shape)
                }
                is AsyncImagePainter.State.Success -> {
                    SubcomposeAsyncImageContent()
                }
                is AsyncImagePainter.State.Error, is AsyncImagePainter.State.Empty -> {
                    // Trigger background repair on error (HTTP 404/403/expired signed URL)
                    LaunchedEffect(track.id) {
                        ArtworkRepairManager.repairIfNeeded(context, track)
                    }

                    // Subtle AMOLED skeleton shimmer while waiting for repair
                    VybeSkeletonBox(modifier = Modifier.fillMaxSize(), shape = shape)
                }
            }
        }
    }
}
