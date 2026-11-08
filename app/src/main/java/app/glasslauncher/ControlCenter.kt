package app.glasslauncher

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import kotlin.math.roundToInt

/** Centro di controllo: tile di vetro, slider verticali luminosità/volume, scorciatoie. */
@Composable
fun ControlCenter(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val backdrop = LocalBackdrop.current
    BackHandler { onClose() }
    val act = ctx.findActivity()
    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVol.toFloat()) }
    var bright by remember {
        mutableFloatStateOf(act?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f)
    }
    val wifiOn = remember { runCatching { (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled }.getOrDefault(false) }
    val btOn = remember { runCatching { (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true }.getOrDefault(false) }
    val airplane = remember { Settings.Global.getInt(ctx.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1 }
    var torch by remember { mutableStateOf(false) }
    val playing = remember { am.isMusicActive }

    fun go(action: String) { openSettings(ctx, action); onClose() }
    fun media(code: Int) {
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)); am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }

    Box(Modifier.fillMaxSize()
        .pointerInput(Unit) { detectTapGestures { onClose() } }
        .pointerInput(Unit) {
            var acc = 0f
            detectVerticalDragGestures(onDragStart = { acc = 0f }) { _, d -> acc += d; if (acc < -80f) { acc = 0f; onClose() } }
        }) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            backdrop.blurred?.let { drawImage(it, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())) }
            drawRect(Color.Black.copy(0.45f))
        }
        Column(
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
                .padding(top = 28.dp, start = 16.dp, end = 16.dp).pointerInput(Unit) { detectTapGestures { } },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glass(Modifier.weight(1f).aspectRatio(1f), RoundedCornerShape(30.dp)) {
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Round(Icons.Rounded.AirplanemodeActive, airplane, Color(0xFFFF9F0A)) { go(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }
                            Round(Icons.Rounded.SignalCellularAlt, !airplane, Color(0xFF30D158)) { go(Settings.ACTION_WIRELESS_SETTINGS) }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Round(Icons.Rounded.Wifi, wifiOn, Color(0xFF0A84FF)) {
                                go(if (Build.VERSION.SDK_INT >= 29) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIFI_SETTINGS)
                            }
                            Round(Icons.Rounded.Bluetooth, btOn, Color(0xFF0A84FF)) { go(Settings.ACTION_BLUETOOTH_SETTINGS) }
                        }
                    }
                }
                Glass(Modifier.weight(1f).aspectRatio(1f), RoundedCornerShape(30.dp)) {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Text(if (playing) "In riproduzione" else "Non in riproduzione", color = Color.White, fontSize = 15.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(34.dp).clickable { media(KeyEvent.KEYCODE_MEDIA_PREVIOUS) })
                            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp).clickable { media(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) })
                            Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(34.dp).clickable { media(KeyEvent.KEYCODE_MEDIA_NEXT) })
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Glass(Modifier.weight(1f).height(170.dp), RoundedCornerShape(30.dp)) {
                    Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Round(Icons.Rounded.ScreenRotation, false, Color.White) { go(Settings.ACTION_DISPLAY_SETTINGS) }
                            Round(Icons.Rounded.DarkMode, false, Color.White) { go(Settings.ACTION_DISPLAY_SETTINGS) }
                        }
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(Color.White.copy(0.18f))
                            .clickable { go(Settings.ACTION_SOUND_SETTINGS) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NightsStay, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp)); Text("Focus", color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
                VSlider(bright, Icons.Rounded.WbSunny, Modifier.width(76.dp).height(170.dp)) {
                    bright = it
                    act?.window?.let { w -> val lp = w.attributes; lp.screenBrightness = it.coerceIn(0.02f, 1f); w.attributes = lp }
                }
                VSlider(vol, Icons.Rounded.VolumeUp, Modifier.width(76.dp).height(170.dp)) {
                    vol = it; am.setStreamVolume(AudioManager.STREAM_MUSIC, (it * maxVol).roundToInt(), 0)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Shortcut(Icons.Rounded.FlashlightOn, torch, Modifier.weight(1f)) { torch = !torch; toggleTorchTo(ctx, torch) }
                Shortcut(Icons.Rounded.Timer, false, Modifier.weight(1f)) {
                    runCatching { ctx.startActivity(Intent(AlarmClock.ACTION_SHOW_TIMERS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }; onClose()
                }
                Shortcut(Icons.Rounded.Calculate, false, Modifier.weight(1f)) {
                    runCatching { ctx.startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }; onClose()
                }
                Shortcut(Icons.Rounded.CameraAlt, false, Modifier.weight(1f)) {
                    runCatching { ctx.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }; onClose()
                }
            }
        }
    }
}

private fun toggleTorchTo(ctx: Context, on: Boolean) {
    runCatching {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val id = cm.cameraIdList.first { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        cm.setTorchMode(id, on)
    }
}

@Composable
private fun Round(icon: ImageVector, active: Boolean, activeColor: Color, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(CircleShape).background(if (active) activeColor else Color.White.copy(0.2f)).clickable { onClick() }, Alignment.Center) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun Shortcut(icon: ImageVector, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Glass(modifier.aspectRatio(1f).clickable { onClick() }, RoundedCornerShape(26.dp)) {
        Box(Modifier.fillMaxSize().background(if (active) Color.White.copy(0.85f) else Color.Transparent), Alignment.Center) {
            Icon(icon, null, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

/** Slider verticale in stile iOS: riempimento bianco dal basso. */
@Composable
private fun VSlider(value: Float, icon: ImageVector, modifier: Modifier, onChange: (Float) -> Unit) {
    var h by remember { mutableFloatStateOf(1f) }
    Glass(modifier.pointerInput(Unit) {
        detectVerticalDragGestures { change, _ -> onChange((1f - change.position.y / h).coerceIn(0f, 1f)) }
    }.pointerInput(Unit) { detectTapGestures { onChange((1f - it.y / h).coerceIn(0f, 1f)) } }, RoundedCornerShape(32.dp)) {
        Box(Modifier.fillMaxSize().onSizeChanged { h = it.height.toFloat().coerceAtLeast(1f) }) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(value.coerceIn(0.02f, 1f)).background(Color.White.copy(0.92f)))
            Icon(icon, null, tint = if (value > 0.12f) Color.Black.copy(0.6f) else Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).size(26.dp))
        }
    }
}
