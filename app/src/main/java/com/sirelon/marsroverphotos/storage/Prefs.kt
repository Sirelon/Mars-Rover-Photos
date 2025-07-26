package com.sirelon.marsroverphotos.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

private const val ThemeKey = "theme"
private const val GridViewKey = "gridView"

/**
 * Created on 29.07.2021 23:54 for Mars-Rover-Photos.
 */
object Prefs {

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("mars-rover-photos", Context.MODE_PRIVATE)
    }

    val themeLiveData = MutableStateFlow<Theme>(Theme.SYSTEM)

    var theme: Theme
        get() {
            val ordinal = sharedPreferences.getInt(ThemeKey, Theme.SYSTEM.ordinal)
            return Theme.entries[ordinal]
        }
        set(value) {
            themeLiveData.value = value
            sharedPreferences.edit {
                putInt(ThemeKey, value.ordinal)
            }
        }

    var gridView: Boolean
        get() = sharedPreferences.getBoolean(GridViewKey, false)
        set(value) {
            sharedPreferences.edit {
                putBoolean(GridViewKey, value)
            }
        }
}

enum class Theme {
    DARK,
    WHITE,
    SYSTEM
}
