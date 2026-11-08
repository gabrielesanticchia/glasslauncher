package app.glasslauncher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun Home(
    apps: List<AppEntry>, homeSignal: Int,
    onSearch: () -> Unit, onControlCenter: () -> Unit, onEdit: () -> Unit,
) {
    val ctx = LocalContext.current
    val backdrop = LocalBackdrop.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val perPage = (((maxHeight - 262.dp) / 92.dp).toInt().coerceIn(3, 6)) * 4
        val dock = remember(apps) { pickDock(ctx, apps) }
        val pages = remember(apps, dock, perPage) {
            apps.filter { a -> dock.none { it.pkg == a.pkg } }.chunked(perPage).ifEmpty { listOf(emptyList()) }
        }
        val total = pages.size + 2                       // widget + pagine app + libreria
        val pager = rememberPagerState(initialPage = 1) { total }
        var category by remember { mutableStateOf<Pair<String, List<AppEntry>>?>(null) }

        LaunchedEffect(homeSignal) { if (homeSignal > 0) { category = null; pager.animateScrollToPage(1) } }
        BackHandler(category != null) { category = null }

        val frac = pager.currentPage + pager.currentPageOffsetFraction
        val libA = (frac - (total - 2)).coerceIn(0f, 1f)
        val wA = (1f - frac).coerceIn(0f, 1f)
        val scrim = max(libA, wA * 0.6f)

        Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onLongPress = { onEdit() }) }) {
            // Sfondo sfocato che compare su pagina widget e Libreria app
            Canvas(Modifier.fillMaxSize()) {
                backdrop.blurred?.let {
                    drawImage(it, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), alpha = scrim)
                    drawRect(Color.Black.copy(0.25f * scrim))
                }
            }
            HorizontalPager(pager, Modifier.fillMaxSize()) { p ->
                when {
                    p == 0 -> WidgetsPage()
                    p == total - 1 -> AppLibrary(apps, onSearch) { t, l -> category = t to l }
                    else -> AppGrid(pages[p - 1])
                }
            }
            StatusBar(Modifier.align(Alignment.TopCenter))
            // Zona in alto a destra: trascina verso il basso (o tocca) per il Centro di controllo
            Box(
                Modifier.align(Alignment.TopEnd).fillMaxWidth(0.45f).height(72.dp)
                    .pointerInput(Unit) {
                        var acc = 0f
                        detectVerticalDragGestures(onDragStart = { acc = 0f }) { _, d ->
                            acc += d; if (acc > 40f) { acc = -1e9f; onControlCenter() }
                        }
                    }
                    .pointerInput(Unit) { detectTapGestures(onTap = { onControlCenter() }) }
            )
            // Pill "Cerca", pallini pagina e dock
            Column(
                Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = 1f - libA }
                    .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                    .padding(bottom = 18.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(total) { i ->
                        Box(Modifier.size(7.dp).clip(CircleShape)
                            .background(Color.White.copy(if (i == pager.currentPage) 1f else 0.4f)))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Glass(Modifier.height(36.dp).width(170.dp).pointerInput(Unit) { detectTapGestures { onSearch() } }, RoundedCornerShape(50)) {
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cerca", color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Glass(Modifier.fillMaxWidth(), RoundedCornerShape(38.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        dock.forEach { AppIcon(it, showLabel = false) }
                    }
                }
            }
            // Cartella/categoria aperta dalla Libreria app
            category?.let { (title, list) ->
                Box(Modifier.fillMaxSize().background(Color.Black.copy(0.35f))
                    .pointerInput(Unit) { detectTapGestures { category = null } }, contentAlignment = Alignment.Center) {
                    Glass(Modifier.fillMaxWidth(0.9f).heightIn(max = 520.dp).pointerInput(Unit) { detectTapGestures { } }, RoundedCornerShape(36.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(14.dp))
                            LazyVerticalGrid(GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(list, key = { it.pkg + it.activity }) { Box(Modifier.fillMaxWidth(), Alignment.Center) { AppIcon(it, 54.dp) } }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppGrid(list: List<AppEntry>) {
    LazyVerticalGrid(
        GridCells.Fixed(4), Modifier.fillMaxSize(), userScrollEnabled = false,
        contentPadding = PaddingValues(top = 72.dp, bottom = 190.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(list, key = { it.pkg + it.activity }) { Box(Modifier.fillMaxWidth(), Alignment.Center) { AppIcon(it) } }
    }
}

@Composable
fun AppIcon(app: AppEntry, size: androidx.compose.ui.unit.Dp = 60.dp, showLabel: Boolean = true) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (pressed) 0.9f else 1f, label = "press")
    val shape = RoundedCornerShape(size * 0.225f)
    Column(
        Modifier.scale(sc).pointerInput(app) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                onTap = { launchApp(ctx, app) },
                onLongPress = { menu = true },
            )
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(app.icon, app.label, Modifier.size(size).clip(shape).border(
            0.8.dp, Brush.linearGradient(listOf(Color.White.copy(0.55f), Color.Transparent, Color.White.copy(0.2f))), shape))
        if (showLabel) {
            Spacer(Modifier.height(4.dp))
            Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center, modifier = Modifier.width(size + 12.dp))
        }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem({ Text("Info app") }, {
                menu = false
                ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
            DropdownMenuItem({ Text("Rimuovi app") }, {
                menu = false
                ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            })
        }
    }
}

@Composable
fun StatusBar(modifier: Modifier = Modifier) {
    val now by rememberNow()
    val (pct, charging) = rememberBattery().value
    Row(
        modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(horizontal = 30.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.SignalCellularAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Icon(Icons.Rounded.Wifi, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Box(Modifier.size(25.dp, 12.dp).border(1.dp, Color.White.copy(0.5f), RoundedCornerShape(4.dp)).padding(1.5.dp)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).clip(RoundedCornerShape(2.5.dp))
                    .background(if (charging) Color(0xFF34C759) else Color.White))
            }
        }
    }
}
