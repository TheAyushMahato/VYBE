package com.vybe.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class ExtractedColors(
    val dominant: Color,
    val darkVibrant: Color,
    val vibrant: Color = dominant,
    val muted: Color = darkVibrant,
    val lightVibrant: Color = vibrant
)

object PaletteUtils {

    private val paletteCache = ConcurrentHashMap<String, ExtractedColors>()

    /**
     * Smart swatch priority with B&W / monochrome detection.
     * Checks dominantSwatch and swatch population first instead of blindly picking a tiny vibrantSwatch
     * (e.g. small red text or logo on a black-and-white cover).
     *
     * If the dominant / largest-population swatches have low saturation (monochrome/B&W/greyscale),
     * returns a clean deep charcoal/AMOLED dark palette (#0A0A0D → #16161B) so the background never turns muddy brown-red.
     *
     * For genuinely colorful covers, extracts the primary color from the highest-population colorful swatch
     * and clamps lightness cleanly (0.12f..0.28f) so the ambient glow stays rich and true to the artwork.
     */
    fun extractColors(bitmap: Bitmap, fallbackDominant: Color = Color(0xFF161616)): ExtractedColors {
        return try {
            val palette = Palette.from(bitmap)
                .maximumColorCount(24)
                .generate()

            val swatchesByPop = palette.swatches.sortedByDescending { it.population }
            val dominantSwatch = palette.dominantSwatch ?: swatchesByPop.firstOrNull()

            if (dominantSwatch == null) {
                return ExtractedColors(
                    dominant = Color(0xFF161616),
                    darkVibrant = Color(0xFF0A0A0A),
                    vibrant = Color(0xFF222222),
                    muted = Color(0xFF101010),
                    lightVibrant = Color(0xFF2A2A2A)
                )
            }

            fun getHsl(swatch: Palette.Swatch): FloatArray {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(swatch.rgb, hsl)
                return hsl
            }

            fun clampSwatch(rgb: Int, satRange: ClosedFloatingPointRange<Float>, lightRange: ClosedFloatingPointRange<Float>): Color {
                val hsl = FloatArray(3)
                ColorUtils.colorToHSL(rgb, hsl)
                hsl[1] = hsl[1].coerceIn(satRange)
                hsl[2] = hsl[2].coerceIn(lightRange)
                return Color(ColorUtils.HSLToColor(hsl))
            }

            val totalPop = swatchesByPop.sumOf { it.population.toLong() }.coerceAtLeast(1L)
            val domHsl = getHsl(dominantSwatch)

            // Find all chromatic swatches (saturation >= 0.20 and not near pure black/white)
            val chromaticSwatches = swatchesByPop.filter { swatch ->
                val hsl = getHsl(swatch)
                hsl[1] >= 0.20f && hsl[2] in 0.08f..0.92f
            }

            val chromaticPop = chromaticSwatches.sumOf { it.population.toLong() }
            val chromaticRatio = chromaticPop.toFloat() / totalPop.toFloat()

            // Highest-population colorful swatch
            val highestPopChromaticSwatch = chromaticSwatches.maxByOrNull { it.population }

            // Check if the dominant and largest-population swatches have low saturation (monochrome/B&W/greyscale covers)
            // e.g. dominant saturation < 0.18f and total chromatic pixels < 15% of the cover,
            // or the top chromatic swatch is tiny (< 18% of dominant swatch population)
            val topSwatches = swatchesByPop.take(3)
            val isMonochrome = (domHsl[1] < 0.18f && chromaticRatio < 0.15f) ||
                (topSwatches.isNotEmpty() && topSwatches.all { getHsl(it)[1] < 0.18f } &&
                 (highestPopChromaticSwatch == null || highestPopChromaticSwatch.population.toFloat() / dominantSwatch.population.toFloat() < 0.18f))

            if (isMonochrome) {
                // Return clean deep charcoal/AMOLED dark palette (#0A0A0D → #16161B)
                return ExtractedColors(
                    dominant = Color(0xFF161616),
                    darkVibrant = Color(0xFF0A0A0A),
                    vibrant = Color(0xFF222222),
                    muted = Color(0xFF101010),
                    lightVibrant = Color(0xFF2A2A2A)
                )
            }

            // For genuinely colorful covers: extract primary from highest-population colorful swatch
            // and clamp lightness cleanly (0.12f..0.28f) so ambient glow stays rich and true to the artwork
            val primaryColorfulSwatch = highestPopChromaticSwatch ?: dominantSwatch

            val primaryColor = clampSwatch(
                primaryColorfulSwatch.rgb,
                satRange = 0.40f..0.85f,
                lightRange = 0.12f..0.28f
            )

            // Deep background tone (darker version of the primary or darkVibrantSwatch clamped)
            val deepSwatch = palette.darkVibrantSwatch ?: primaryColorfulSwatch
            val deepColor = clampSwatch(
                deepSwatch.rgb,
                satRange = 0.35f..0.75f,
                lightRange = 0.06f..0.16f
            )

            // Vibrant accent for highlights / seekbar
            val vibrantSwatch = palette.vibrantSwatch ?: primaryColorfulSwatch
            val vibColor = clampSwatch(
                vibrantSwatch.rgb,
                satRange = 0.45f..0.85f,
                lightRange = 0.30f..0.52f
            )

            // Muted tone
            val mutedSwatch = palette.mutedSwatch ?: deepSwatch
            val mutColor = clampSwatch(
                mutedSwatch.rgb,
                satRange = 0.25f..0.55f,
                lightRange = 0.14f..0.26f
            )

            // Light vibrant accent
            val lightVibColor = palette.lightVibrantSwatch?.let {
                clampSwatch(it.rgb, 0.40f..0.80f, 0.45f..0.68f)
            } ?: clampSwatch(primaryColorfulSwatch.rgb, 0.40f..0.80f, 0.45f..0.60f)

            ExtractedColors(
                dominant = primaryColor,
                darkVibrant = deepColor,
                vibrant = vibColor,
                muted = mutColor,
                lightVibrant = lightVibColor
            )
        } catch (e: Exception) {
            Log.e("PaletteUtils", "Failed to extract palette from bitmap: ${e.message}")
            ExtractedColors(
                dominant = Color(0xFF161616),
                darkVibrant = Color(0xFF0A0A0A),
                vibrant = Color(0xFF222222),
                muted = Color(0xFF101010),
                lightVibrant = Color(0xFF2A2A2A)
            )
        }
    }

    suspend fun extractColorsFromUrl(
        context: Context,
        imageUrl: String,
        fallbackDominant: Color = Color(0xFF1A1A1A)
    ): ExtractedColors = withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) {
            return@withContext ExtractedColors(fallbackDominant, Color(0xFF121212))
        }

        // Cache hit: instant return, zero disk/CPU overhead
        paletteCache[imageUrl]?.let { cached ->
            return@withContext cached
        }

        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false) // CRITICAL: Software bitmap so Palette.from never fails silently
                .size(300) // slightly larger sample for 24-swatch accuracy
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    // Strictly run CPU-heavy palette calculation on Dispatchers.Default
                    val colors = withContext(Dispatchers.Default) {
                        extractColors(bitmap, fallbackDominant)
                    }
                    paletteCache[imageUrl] = colors
                    Log.d(
                        "PaletteUtils",
                        "SmartPalette: primary=#${Integer.toHexString(colors.dominant.toArgb()).uppercase()}, " +
                        "deep=#${Integer.toHexString(colors.darkVibrant.toArgb()).uppercase()}, " +
                        "vibrant=#${Integer.toHexString(colors.vibrant.toArgb()).uppercase()}"
                    )
                    return@withContext colors
                }
            }
            ExtractedColors(fallbackDominant, Color(0xFF121212))
        } catch (e: Exception) {
            Log.e("PaletteUtils", "Error loading image for palette extraction: ${e.message}")
            ExtractedColors(fallbackDominant, Color(0xFF121212))
        }
    }

    fun extractDynamicColor(bitmap: Bitmap, fallback: Color): Color {
        return extractColors(bitmap, fallback).dominant
    }

    suspend fun extractColorFromUrl(
        context: Context,
        imageUrl: String,
        fallbackColor: Color
    ): Color = extractColorsFromUrl(context, imageUrl, fallbackColor).dominant
}
