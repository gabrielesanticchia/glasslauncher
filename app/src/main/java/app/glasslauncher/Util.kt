package app.glasslauncher

import android.app.Activity
import android.app.ActivityOptions
import android.content.*
import android.content.pm.ApplicationInfo
import android.os.BatteryManager
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.time.LocalDateTime

data class AppEntry(
    val label: String, val pkg: String, val activity: String,
    val icon: ImageBitmap, val category: Int, val installed: Long,
)

fun loadApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .map { ri ->
            val ai = ri.activityInfo
            val installed = runCatching { pm.getPackageInfo(ai.packageName, 0).firstInstallTime }.getOrDefault(0L)
            val cat = if (android.os.Build.VERSION.SDK_INT >= 26) ai.applicationInfo.category else -1
            AppEntry(ri.loadLabel(pm).toString(), ai.packageName, ai.name,
                ri.loadIcon(pm).toBitmap(192, 192).asImageBitmap(), cat, installed)
        }
        .sortedBy { it.label.lowercase() }
}

fun launchApp(ctx: Context, app: AppEntry) {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(app.pkg, app.activity))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    val opts = ActivityOptions.makeCustomAnimation(ctx, android.R.anim.fade_in, android.R.anim.fade_out).toBundle()
    runCatching { ctx.startActivity(i, opts) }
}

fun openSettings(ctx: Context, action: String) {
    runCatching { ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

/** Dock di default: telefono, messaggi, browser, fotocamera (se risolvibili). */
fun pickDock(ctx: Context, apps: List<AppEntry>): List<AppEntry> {
    val pm = ctx.packageManager
    val intents = listOf(
        Intent(Intent.ACTION_DIAL),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_BROWSER),
        Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
    )
    val res = intents.mapNotNull { i ->
        val pkg = pm.resolveActivity(i, 0)?.activityInfo?.packageName
        apps.firstOrNull { it.pkg == pkg }
    }.distinctBy { it.pkg }
    return (res + apps.filter { a -> res.none { it.pkg == a.pkg } }).take(4)
}

@Composable
fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
    }
}

/** Livello batteria (%) e stato di ricarica. */
@Composable
fun rememberBattery(): State<Pair<Int, Boolean>> {
    val ctx = LocalContext.current
    return produceState(Pair(100, false)) {
        while (true) {
            val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            if (i != null) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                value = Pair(l * 100 / s, st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL)
            }
            delay(20_000)
        }
    }
}
