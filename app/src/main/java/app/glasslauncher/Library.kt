package app.glasslauncher

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private fun categoryOf(a: AppEntry): String = when (a.category) {
    ApplicationInfo.CATEGORY_GAME -> "Giochi"
    ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> "Intrattenimento"
    ApplicationInfo.CATEGORY_SOCIAL -> "Social"
    ApplicationInfo.CATEGORY_NEWS -> "Informazione e lettura"
    ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Produttività"
    else -> "Utility"
}

/** App Library: gruppi automatici per categoria + ricerca. */
@Composable
fun AppLibrary(apps: List<AppEntry>, onSearch: () -> Unit, onOpen: (String, List<AppEntry>) -> Unit) {
    val groups = remember(apps) {
        val recent = apps.sortedByDescending { it.installed }.take(8)
        listOf("Aggiunte di recente" to recent) +
            apps.groupBy(::categoryOf).toList().sortedBy { it.first }
    }.filter { it.second.isNotEmpty() }
    Column(Modifier.fillMaxSize().padding(top = 72.dp, start = 16.dp, end = 16.dp)) {
        Glass(Modifier.fillMaxWidth().height(44.dp).pointerInput(Unit) { detectTapGestures { onSearch() } }, RoundedCornerShape(50)) {
            Row(Modifier.padding(horizontal = 16.dp).align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.8f))
                Spacer(Modifier.width(8.dp))
                Text("Libreria app", color = Color.White.copy(0.8f), fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            groups.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { (title, list) -> Box(Modifier.weight(1f)) { CategoryCard(title, list) { onOpen(title, list) } } }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(title: String, list: List<AppEntry>, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    Column {
        Glass(Modifier.fillMaxWidth().pointerInput(list) { detectTapGestures { onOpen() } }, RoundedCornerShape(30.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val big = list.take(3)
                val rest = list.drop(3).take(4)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Slot(big.getOrNull(0)); Slot(big.getOrNull(1))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Slot(big.getOrNull(2))
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                            for (r in 0..1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (c in 0..1) rest.getOrNull(r * 2 + c)?.let { a ->
                                    Image(a.icon, null, Modifier.fillMaxWidth(0.46f).aspectRatio(1f).clip(RoundedCornerShape(9.dp)))
                                } ?: Spacer(Modifier.fillMaxWidth(0.46f))
                            }
                        }
                    }
                }
            }
        }
        Text(title, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
    }
}

@Composable
private fun RowScope.Slot(app: AppEntry?) {
    Box(Modifier.weight(1f).aspectRatio(1f), Alignment.Center) {
        if (app != null) AppIcon(app, 56.dp, showLabel = false)
    }
}

/** Ricerca stile Spotlight / ricerca della Libreria app. */
@Composable
fun SearchOverlay(apps: List<AppEntry>, onClose: () -> Unit) {
    val backdrop = LocalBackdrop.current
    var q by remember { mutableStateOf("") }
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    val ctx = LocalContext.current
    val list = remember(q, apps) { if (q.isBlank()) apps else apps.filter { it.label.contains(q, true) } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(0.55f)).pointerInput(Unit) { detectTapGestures { } }) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility).imePadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Glass(Modifier.weight(1f).height(48.dp), RoundedCornerShape(50)) {
                    Row(Modifier.padding(horizontal = 16.dp).align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Search, null, tint = Color.White.copy(0.8f))
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(q, { q = it }, Modifier.focusRequester(fr).fillMaxWidth(), singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 17.sp), cursorBrush = SolidColor(Color.White))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("Annulla", color = Color.White, fontSize = 16.sp, modifier = Modifier.pointerInput(Unit) { detectTapGestures { onClose() } })
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn {
                items(list, key = { it.pkg + it.activity }) { a ->
                    Row(Modifier.fillMaxWidth().pointerInput(a) { detectTapGestures { launchApp(ctx, a); onClose() } }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Image(a.icon, null, Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)))
                        Spacer(Modifier.width(14.dp))
                        Text(a.label, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
