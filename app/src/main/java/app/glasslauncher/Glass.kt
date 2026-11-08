package app.glasslauncher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

val LocalPrefs = staticCompositionLocalOf<LauncherPrefs> { error("LauncherPrefs mancante") }

/** Sfondo già sfocato, condiviso da tutti i pannelli di vetro. */
class GlassBackdrop(val blurred: ImageBitmap?, val root: IntSize)
val LocalBackdrop = compositionLocalOf { GlassBackdrop(null, IntSize.Zero) }

private val saturate = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.45f) })

/**
 * Liquid Glass: ogni pannello disegna la porzione di sfondo sfocato che sta dietro di sé,
 * leggermente ingrandita (effetto lente/rifrazione), saturata, con velo chiaro, riflesso
 * superiore e bordo luminoso. Come in iOS 27 i riflessi sono statici (non reagiscono al movimento).
 * La trasparenza è regolata dallo slider in Personalizza.
 */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    strength: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val backdrop = LocalBackdrop.current
    val t = LocalPrefs.current.glass
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInRoot() }
            .clip(shape)
            .drawWithContent {
                val img = backdrop.blurred
                if (img != null && backdrop.root.width > 0) {
                    val k = img.width.toFloat() / backdrop.root.width
                    val sw = size.width * k; val sh = size.height * k
                    val cw = sw * 0.95f; val ch = sh * 0.95f
                    val sx = (origin.x * k + (sw - cw) / 2f).roundToInt().coerceIn(0, img.width - 1)
                    val sy = (origin.y * k + (sh - ch) / 2f).roundToInt().coerceIn(0, img.height - 1)
                    drawImage(
                        img, srcOffset = IntOffset(sx, sy),
                        srcSize = IntSize(cw.roundToInt().coerceIn(1, img.width - sx), ch.roundToInt().coerceIn(1, img.height - sy)),
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                        colorFilter = saturate,
                    )
                }
                drawRect(Color.White.copy(alpha = ((0.04f + 0.34f * t) * strength).coerceIn(0f, 1f)))
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(0.16f * strength), Color.Transparent), endY = size.height * 0.55f))
                drawContent()
                drawOutline(
                    shape.createOutline(size, layoutDirection, this),
                    Brush.linearGradient(
                        listOf(Color.White.copy(0.8f), Color.White.copy(0.06f), Color.White.copy(0.3f)),
                        start = Offset.Zero, end = Offset(size.width, size.height),
                    ),
                    style = Stroke(1.2.dp.toPx()),
                )
            },
        content = content,
    )
}
