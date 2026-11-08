package app.glasslauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val Red = Color(0xFFFF453A)
private val R24 = RoundedCornerShape(26.dp)

/** Pagina widget a sinistra (Today View). Widget disegnati in Compose; vedi README per AppWidgetHost. */
@Composable
fun WidgetsPage() {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = 76.dp, bottom = 40.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DateWidget(Modifier.weight(1f).aspectRatio(1f))
            BatteryWidget(Modifier.weight(1f).aspectRatio(1f))
        }
        WeatherWidget(Modifier.fillMaxWidth().height(120.dp))
        CalendarXL(Modifier.fillMaxWidth())   // widget extra-large, novità di iOS 27
        MusicWidget(Modifier.fillMaxWidth().height(96.dp))
    }
}

@Composable
fun DateWidget(modifier: Modifier) {
    val now by rememberNow()
    Glass(modifier, R24) {
        Column(Modifier.padding(16.dp)) {
            Text(now.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase(), color = Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${now.dayOfMonth}", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Light)
            Text(now.month.getDisplayName(TextStyle.FULL, Locale.getDefault()), color = Color.White.copy(0.8f), fontSize = 14.sp)
        }
    }
}

@Composable
fun BatteryWidget(modifier: Modifier) {
    val (pct, charging) = rememberBattery().value
    Glass(modifier, R24) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Batteria", color = Color.White.copy(0.8f), fontSize = 13.sp)
            Text("$pct%", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Light)
            Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White.copy(0.25f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(pct / 100f).clip(CircleShape)
                    .background(if (charging) Color(0xFF34C759) else Color.White))
            }
        }
    }
}

/** Dati di esempio: collega un'API meteo per dati reali. */
@Composable
fun WeatherWidget(modifier: Modifier) {
    Glass(modifier, R24) {
        Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Meteo", color = Color.White.copy(0.8f), fontSize = 13.sp)
                Text("21°", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Light)
                Text("Parz. soleggiato", color = Color.White.copy(0.8f), fontSize = 13.sp)
            }
            Icon(Icons.Rounded.WbSunny, null, tint = Color(0xFFFFD60A), modifier = Modifier.size(56.dp))
        }
    }
}

@Composable
fun CalendarXL(modifier: Modifier) {
    val now by rememberNow()
    val today = now.toLocalDate()
    val first = today.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - 1               // lunedì = 0
    val days = today.lengthOfMonth()
    Glass(modifier, R24) {
        Column(Modifier.padding(18.dp)) {
            Text(today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).uppercase() + " ${today.year}",
                color = Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.values().forEach {
                    Text(it.getDisplayName(TextStyle.NARROW, Locale.getDefault()), Modifier.weight(1f), color = Color.White.copy(0.6f),
                        fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
            val cells = offset + days
            for (r in 0 until (cells + 6) / 7) {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    for (c in 0 until 7) {
                        val d = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f).height(30.dp), Alignment.Center) {
                            if (d in 1..days) {
                                val isToday = d == today.dayOfMonth
                                Box(Modifier.size(28.dp).clip(CircleShape).background(if (isToday) Red else Color.Transparent), Alignment.Center) {
                                    Text("$d", color = Color.White, fontSize = 14.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MusicWidget(modifier: Modifier) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val am = remember { ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    fun key(code: Int) {
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, code))
    }
    Glass(modifier, R24) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Riproduzione", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                listOf(Icons.Rounded.SkipPrevious to android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                    Icons.Rounded.PlayArrow to android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                    Icons.Rounded.SkipNext to android.view.KeyEvent.KEYCODE_MEDIA_NEXT).forEach { (ic, code) ->
                    Icon(ic, null, tint = Color.White, modifier = Modifier.size(34.dp).clip(CircleShape)
                        .androidxClick { key(code) })
                }
            }
        }
    }
}

private fun Modifier.androidxClick(onClick: () -> Unit): Modifier =
    this.then(Modifier.clickable { onClick() })
