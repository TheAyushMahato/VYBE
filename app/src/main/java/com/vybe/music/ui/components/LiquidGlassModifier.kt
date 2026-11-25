package com.vybe.music.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

val FrostedPillShape: Shape = RoundedCornerShape(300.dp)

private const val LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float2 uResolution;
uniform float uRadius;

vec4 main(vec2 fragCoord) {
    vec2 halfRes = uResolution * 0.5;
    vec2 p = abs(fragCoord - halfRes);
    vec2 b = halfRes - vec2(uRadius, uRadius);
    vec2 q = p - b;
    
    // Signed distance to capsule / rounded box
    float d = length(max(q, vec2(0.0, 0.0))) + min(max(q.x, q.y), 0.0) - uRadius;
    
    // Smooth anti-aliased edge cutout
    float edgeAlpha = clamp(0.5 - d, 0.0, 1.0);
    if (edgeAlpha <= 0.0) {
        return vec4(0.0, 0.0, 0.0, 0.0);
    }
    
    // Outward surface normal
    vec2 n = vec2(0.0, 0.0);
    vec2 cornerOffset = max(q, vec2(0.0, 0.0));
    float len = length(cornerOffset);
    if (len > 0.001) {
        n = (cornerOffset / len) * sign(fragCoord - halfRes);
    } else {
        if (q.x > q.y) {
            n = vec2(sign(fragCoord.x - halfRes.x), 0.0);
        } else {
            n = vec2(0.0, sign(fragCoord.y - halfRes.y));
        }
    }
    
    // 1. Subtle Edge Refraction (Lens Distortion)
    // Refract only near perimeter (< 14px inward), leaves center icons/text 100% distortion-free
    float refractDepth = min(uRadius * 0.5, 14.0);
    float edgeDist = clamp(-d / refractDepth, 0.0, 1.0);
    float refractFactor = (1.0 - edgeDist) * (1.0 - edgeDist);
    float refractStrength = 4.0 * refractFactor;
    vec2 sampleCoord = fragCoord - n * refractStrength;
    sampleCoord = clamp(sampleCoord, vec2(0.0, 0.0), uResolution);
    
    vec4 color = content.eval(sampleCoord);
    
    // 2. High-Contrast Translucent Dark Tint
    // Darkens background for strong contrast behind icons, but preserves pure #FFFFFF and vivid icon colors
    float maxC = max(max(color.r, color.g), color.b);
    float minC = min(min(color.r, color.g), color.b);
    float isForeground = step(0.85, maxC) * step(0.15, (maxC - minC) + step(0.95, minC));
    float tintAmount = mix(0.15, 0.0, isForeground);
    color.rgb = mix(color.rgb, vec3(0.0, 0.0, 0.0), tintAmount);
    
    // 3. Thin Specular Rim Highlight (Top-Left illumination + subtle Bottom-Right reflection)
    vec2 dirToLight = vec2(-0.707106, -0.707106);
    float dotTL = max(dot(n, dirToLight), 0.0);
    float dotBR = max(dot(-n, dirToLight), 0.0);
    
    float rimMask = smoothstep(-1.8, -0.2, d) * smoothstep(0.5, -0.2, d);
    float specularTL = pow(dotTL, 2.5) * 0.32 * rimMask;
    float specularBR = pow(dotBR, 2.5) * 0.10 * rimMask;
    float totalSpec = specularTL + specularBR;
    
    color.rgb += vec3(totalSpec, totalSpec, totalSpec);
    
    return color * edgeAlpha;
}
"""

/**
 * Universal Liquid Glass Modifier:
 * - Android 13+ (API 33+): Hardware AGSL RuntimeShader with subtle lens refraction,
 *   crisp specular rim highlights, and high-contrast translucent dark tint.
 * - Android 12 and below fallback: Live Haze backdrop blur + razor-thin gradient rim border.
 * - Smooth 60fps scrolling: RuntimeShader and RenderEffect are compiled once and cached across draws.
 */
fun Modifier.liquidGlass(
    hazeState: HazeState? = null,
    shape: Shape = FrostedPillShape,
    blurRadius: Dp = 24.dp,
    tint: Color = Color.Unspecified,
    blackAlpha: Float = 0.18f,
    whiteAlpha: Float = 0.04f,
    borderWidth: Dp = 0.75.dp,
    cornerRadius: Dp? = null
): Modifier = composed {
    val hazeTint = remember(tint, blackAlpha, hazeState) {
        if (tint != Color.Unspecified) {
            tint
        } else if (hazeState != null) {
            Color.Black.copy(alpha = blackAlpha)
        } else {
            Color(0xFF141414).copy(alpha = 0.88f)
        }
    }
    val rimBrush = remember(borderWidth) {
        Brush.verticalGradient(
            0.00f to Color.White.copy(alpha = 0.20f),
            1.00f to Color.White.copy(alpha = 0.10f)
        )
    }

    val baseModifier = this
        .clip(shape)
        .then(
            if (hazeState != null) {
                Modifier
                    .hazeChild(
                        state = hazeState,
                        shape = shape,
                        style = HazeStyle(
                            tint = hazeTint,
                            blurRadius = blurRadius,
                            noiseFactor = 0f
                        )
                    )
                    .background(Color.White.copy(alpha = whiteAlpha), shape)
            } else {
                Modifier
                    .background(hazeTint, shape)
                    .background(Color.White.copy(alpha = whiteAlpha), shape)
            }
        )

    val shader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                android.graphics.RuntimeShader(LIQUID_GLASS_AGSL)
            } catch (_: Throwable) {
                null
            }
        } else {
            null
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shader != null) {
        var cachedRenderEffect by remember { mutableStateOf<androidx.compose.ui.graphics.RenderEffect?>(null) }
        var cachedWidth by remember { mutableFloatStateOf(-1f) }
        var cachedHeight by remember { mutableFloatStateOf(-1f) }

        baseModifier.graphicsLayer {
            val w = size.width
            val h = size.height
            if (w > 0f && h > 0f) {
                if (w != cachedWidth || h != cachedHeight || cachedRenderEffect == null) {
                    cachedWidth = w
                    cachedHeight = h
                    val r = cornerRadius?.toPx() ?: (minOf(w, h) * 0.5f)
                    shader.setFloatUniform("uResolution", w, h)
                    shader.setFloatUniform("uRadius", r)
                    cachedRenderEffect = android.graphics.RenderEffect
                        .createRuntimeShaderEffect(shader, "content")
                        .asComposeRenderEffect()
                }
                renderEffect = cachedRenderEffect
            }
        }
    } else {
        baseModifier
    }
}

/**
 * Drop shadow specification:
 * offsetY = 10.dp, blurRadius = 40.dp, color #000000 at 40% alpha (45% on collapsed circular nav button).
 */
fun Modifier.glassDockShadow(
    shape: Shape = FrostedPillShape,
    elevation: Dp = 16.dp,
    alpha: Float = 0.40f
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = alpha * 0.8f),
    spotColor = Color.Black.copy(alpha = alpha)
)
