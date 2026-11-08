package app.glasslauncher

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Impostazioni persistenti. I valori sono osservabili da Compose. */
class LauncherPrefs(private val sp: SharedPreferences) {
    /** Trasparenza Liquid Glass (stile slider di iOS 27): 0 = molto trasparente, 1 = vetro opaco/smerigliato. */
    var glass by mutableFloatStateOf(sp.getFloat("glass", 0.5f))
    var clockStyle by mutableIntStateOf(sp.getInt("clockStyle", 0))
    /** Altezza dell'orologio nel blocco schermo, come frazione dell'altezza dello schermo. */
    var clockY by mutableFloatStateOf(sp.getFloat("clockY", 0.14f))
    var clockScale by mutableFloatStateOf(sp.getFloat("clockScale", 1f))
    var clockColor by mutableIntStateOf(sp.getInt("clockColor", 0))
    var lockEnabled by mutableStateOf(sp.getBoolean("lockEnabled", true))
    var wallpaperVersion by mutableIntStateOf(sp.getInt("wpv", 0))

    fun commit() {
        sp.edit()
            .putFloat("glass", glass).putInt("clockStyle", clockStyle)
            .putFloat("clockY", clockY).putFloat("clockScale", clockScale)
            .putInt("clockColor", clockColor).putBoolean("lockEnabled", lockEnabled)
            .putInt("wpv", wallpaperVersion).apply()
    }
}
