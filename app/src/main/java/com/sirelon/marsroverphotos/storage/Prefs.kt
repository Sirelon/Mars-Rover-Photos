package com.sirelon.marsroverphotos.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow

private const val THEME_KEY = "theme"
private const val GRID_VIEW_KEY = "gridView"

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
            val ordinal = sharedPreferences.getInt(THEME_KEY, Theme.SYSTEM.ordinal)
            return Theme.entries[ordinal]
        }
        set(value) {
            themeLiveData.value = value
            sharedPreferences.edit {
                putInt(THEME_KEY, value.ordinal)
            }
        }

    var gridView: Boolean
        get() = sharedPreferences.getBoolean(GRID_VIEW_KEY, false)
        set(value) {
            sharedPreferences.edit {
                putBoolean(GRID_VIEW_KEY, value)
            }
        }
}

enum class Theme {
    DARK,
    WHITE,
    SYSTEM
}
