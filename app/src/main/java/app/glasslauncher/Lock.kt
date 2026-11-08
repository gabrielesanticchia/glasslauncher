package app.glasslauncher

import android.content.Intent
import android.provider.MediaStore
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Stili orologio: in iOS 27 c'è anche il layout "compatto" lungo il bordo superiore. */
enum class ClockStyle(val label: String, val family: FontFamily, val weight: FontWeight) {
    STACK("Impilato", FontFamily.SansSerif, FontWeight.Bold),
    THIN("Sottile", FontFamily.SansSerif, FontWeight.ExtraLight),
    SERIF("Serif", FontFamily.Serif, FontWeight.Medium),
    MONO("Mono", FontFamily.Monospace, FontWeight.Normal),
    CASUAL("Corsivo", FontFamily.Cursive, FontWeight.Normal),
    COMPACT("Compatto", FontFamily.SansSerif, FontWeight.SemiBold),
}
val clockColors = listOf(Color.White, Color(0xFFFFD60A), Color(0xFFFF9F0A), Color(0xFFFF375F), Color(0xFF64D2FF), Color(0xFF30D158))

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    val prefs = LocalPrefs.current
    val ctx = LocalContext.current
    val h = LocalConfiguration.current.screenHeightDp
    val now by rememberNow()
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var customizing by remember { mutableStateOf(false) }
    val style = ClockStyle.values()[prefs.clockStyle.coerceIn(0, ClockStyle.values().size - 1)]
    val color = clockColors[prefs.clockColor.coerceIn(0, clockColors.size - 1)]
    val glassText = TextStyle(color = color.copy(alpha = 0.92f), shadow = Shadow(Color.Black.copy(0.25f), blurRadius = 18f))

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { translationY = drag.value; alpha = 1f + drag.value / (size.height * 1.2f) }
            .pointerInput(Unit) { detectTapGestures(onLongPress = { customizing = true }) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (drag.value < -size.height * 0.22f) { drag.animateTo(-size.height.toFloat()); onUnlock() }
                            else drag.animateTo(0f)
                        }
                    },
                ) { _, d -> scope.launch { drag.snapTo((drag.value + d).coerceAtMost(0f)) } }
            },
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(0.12f)))
        Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.align(Alignment.TopCenter)
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility).padding(top = 18.dp).size(18.dp))

        val date = now.format(DateTimeFormatter.ofPattern("EEE d MMM"))
        val sc = prefs.clockScale
        if (style == ClockStyle.COMPACT) {
            Column(Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility).padding(top = 52.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date, style = glassText, fontSize = (15 * sc).sp, fontWeight = FontWeight.Medium)
                Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), style = glassText, fontSize = (34 * sc).sp, fontFamily = style.family, fontWeight = style.weight)
            }
        } else {
            // Altezza personalizzabile: clockY è la frazione dello schermo
            Column(Modifier.align(Alignment.TopCenter).offset { IntOffset(0, (h * prefs.clockY * density).roundToInt()) },
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date, style = glassText, fontSize = (20 * sc).sp, fontWeight = FontWeight.SemiBold)
                if (style == ClockStyle.THIN) {
                    Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), style = glassText, fontSize = (92 * sc).sp, fontFamily = style.family, fontWeight = style.weight)
                } else {
                    val big = (118 * sc).sp
                    Text(now.format(DateTimeFormatter.ofPattern("HH")), style = glassText, fontSize = big, lineHeight = big * 0.86f, fontFamily = style.family, fontWeight = style.weight)
                    Text(now.format(DateTimeFormatter.ofPattern("mm")), style = glassText, fontSize = big, lineHeight = big * 0.86f, fontFamily = style.family, fontWeight = style.weight)
                }
            }
        }

        Row(Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .fillMaxWidth().padding(horizontal = 44.dp, vertical = 44.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Glass(Modifier.size(54.dp).clickable { toggleTorch(ctx) }, CircleShape) {
                Icon(Icons.Rounded.FlashlightOn, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
            }
            Glass(Modifier.size(54.dp).clickable {
                runCatching { ctx.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, CircleShape) {
                Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }
        Text("Scorri verso l'alto per sbloccare", color = Color.White.copy(0.7f), fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility).padding(bottom = 14.dp))

        if (customizing) ClockCustomizer(prefs) { customizing = false; prefs.commit() }
    }
}

private var torchOn = false
fun toggleTorch(ctx: android.content.Context) {
    runCatching {
        val cm = ctx.getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        val id = cm.cameraIdList.first { cm.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        torchOn = !torchOn
        cm.setTorchMode(id, torchOn)
    }
}

/** Editor del blocco schermo (appare con pressione prolungata, come in iOS). */
@Composable
fun ClockCustomizer(prefs: LauncherPrefs, onDone: () -> Unit) {
    val compact = prefs.clockStyle == ClockStyle.COMPACT.ordinal
    Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { } }, contentAlignment = Alignment.BottomCenter) {
        Glass(Modifier.fillMaxWidth().padding(12.dp), RoundedCornerShape(34.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Personalizza orologio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ClockStyle.values().forEach { s ->
                        val sel = prefs.clockStyle == s.ordinal
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(if (sel) 0.4f else 0.15f))
                            .clickable { prefs.clockStyle = s.ordinal }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(s.label, color = Color.White, fontFamily = s.family, fontSize = 15.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    clockColors.forEachIndexed { i, c ->
                        Box(Modifier.size(30.dp).clip(CircleShape).background(c).clickable { prefs.clockColor = i }) {
                            if (prefs.clockColor == i) Box(Modifier.align(Alignment.Center).size(10.dp).clip(CircleShape).background(Color.Black.copy(0.4f)))
                        }
                    }
                }
                Text(if (compact) "Altezza (fissa nel layout compatto)" else "Altezza", color = Color.White.copy(0.85f), fontSize = 13.sp)
                GlassSlider(prefs.clockY, { prefs.clockY = it }, 0.04f..0.5f, enabled = !compact)
                Text("Dimensione", color = Color.White.copy(0.85f), fontSize = 13.sp)
                GlassSlider(prefs.clockScale, { prefs.clockScale = it }, 0.6f..1.4f)
                Box(Modifier.align(Alignment.End).clip(RoundedCornerShape(50)).background(Color.White.copy(0.9f))
                    .clickable { onDone() }.padding(horizontal = 22.dp, vertical = 8.dp)) {
                    Text("Fine", color = Color.Black, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun GlassSlider(value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, enabled: Boolean = true) {
    Slider(value, onChange, Modifier.alpha(if (enabled) 1f else 0.4f), enabled = enabled, valueRange = range,
        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(0.3f)))
}
