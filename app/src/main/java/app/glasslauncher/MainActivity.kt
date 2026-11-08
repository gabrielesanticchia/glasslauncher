package app.glasslauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val prefs by lazy { LauncherPrefs(getSharedPreferences("launcher", MODE_PRIVATE)) }
    private val homeSignal = mutableIntStateOf(0)
    private val locked = mutableStateOf(true)

    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) { if (prefs.lockEnabled) locked.value = true }
    }
    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { Wallpaper.save(this, uri); prefs.wallpaperVersion++; prefs.commit() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) window.attributes.layoutInDisplayCutoutMode =
            android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        locked.value = prefs.lockEnabled
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
        setContent { LauncherRoot(prefs, locked, homeSignal.intValue) { picker.launch("image/*") } }
    }

    override fun onNewIntent(intent: Intent) {   // tasto Home: torna alla prima pagina
        super.onNewIntent(intent)
        homeSignal.intValue++
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() { unregisterReceiver(screenOff); super.onDestroy() }
}

@Composable
fun LauncherRoot(prefs: LauncherPrefs, locked: MutableState<Boolean>, homeSignal: Int, onPickWallpaper: () -> Unit) {
    val ctx = LocalContext.current
    val dens = LocalDensity.current
    var appsVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { appsVersion++ } }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED); addDataScheme("package")
        }
        ctx.registerReceiver(r, f)
        onDispose { ctx.unregisterReceiver(r) }
    }
    val apps by produceState(emptyList<AppEntry>(), appsVersion) { value = withContext(Dispatchers.IO) { loadApps(ctx) } }

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val wPx = with(dens) { maxWidth.roundToPx() }
        val hPx = with(dens) { maxHeight.roundToPx() }
        val wp by produceState<Pair<androidx.compose.ui.graphics.ImageBitmap, androidx.compose.ui.graphics.ImageBitmap>?>(
            null, wPx, hPx, prefs.wallpaperVersion
        ) {
            value = withContext(Dispatchers.Default) {
                val bmp = Wallpaper.load(ctx, wPx, hPx)
                bmp.asImageBitmap() to Wallpaper.blurred(bmp).asImageBitmap()
            }
        }
        var showCC by remember { mutableStateOf(false) }
        var showSearch by remember { mutableStateOf(false) }
        var showEdit by remember { mutableStateOf(false) }
        LaunchedEffect(homeSignal) { showCC = false; showSearch = false; showEdit = false }

        CompositionLocalProvider(
            LocalPrefs provides prefs,
            LocalBackdrop provides GlassBackdrop(wp?.second, IntSize(wPx, hPx)),
        ) {
            wp?.first?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Home(apps, homeSignal, { showSearch = true }, { showCC = true }, { showEdit = true })

            AnimatedVisibility(showCC, enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(0.9f, 0f), initialScale = 0.5f),
                exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(0.9f, 0f), targetScale = 0.5f)) {
                ControlCenter { showCC = false }
            }
            AnimatedVisibility(showSearch, enter = fadeIn(tween(180)), exit = fadeOut(tween(120))) {
                BackHandler { showSearch = false }
                SearchOverlay(apps) { showSearch = false }
            }
            if (showEdit) EditSheet(prefs, onPickWallpaper) { showEdit = false; prefs.commit() }
            AnimatedVisibility(locked.value, enter = fadeIn(), exit = fadeOut(tween(150))) {
                LockScreen { locked.value = false }
            }
        }
    }
}

/** Personalizza: slider Liquid Glass (come in iOS 27), sfondo, blocco schermo, launcher predefinito. */
@Composable
fun EditSheet(prefs: LauncherPrefs, onPickWallpaper: () -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    BackHandler { onClose() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.4f)).pointerInput(Unit) { detectTapGestures { onClose() } }, Alignment.Center) {
        Glass(Modifier.fillMaxWidth(0.9f).pointerInput(Unit) { detectTapGestures { } }, RoundedCornerShape(34.dp)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Personalizza", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Liquid Glass", color = Color.White.copy(0.85f), fontSize = 14.sp)
                GlassSlider(prefs.glass, { prefs.glass = it }, 0f..1f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Trasparente", color = Color.White.copy(0.6f), fontSize = 12.sp)
                    Text("Smerigliato", color = Color.White.copy(0.6f), fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Blocco schermo", color = Color.White, fontSize = 16.sp)
                    Switch(prefs.lockEnabled, { prefs.lockEnabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF34C759)))
                }
                SheetButton("Cambia sfondo") { onPickWallpaper(); onClose() }
                SheetButton("Imposta come launcher predefinito") {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                SheetButton("Fine", primary = true) { onClose() }
            }
        }
    }
}

@Composable
private fun SheetButton(text: String, primary: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().background(if (primary) Color.White.copy(0.9f) else Color.White.copy(0.18f), RoundedCornerShape(50))
        .clickable { onClick() }.padding(vertical = 12.dp), Alignment.Center) {
        Text(text, color = if (primary) Color.Black else Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
