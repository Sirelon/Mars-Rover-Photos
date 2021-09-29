package com.sirelon.marsroverphotos.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import timber.log.Timber

/**
 * Created on 29.07.2021 23:54 for Mars-Rover-Photos.
 */
object Prefs {

    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("mars-rover-photos", Context.MODE_PRIVATE)
    }

    val themeLiveData = MutableLiveData<Theme>()

    init {
        themeLiveData.observeForever {
            Timber.d("$it called");
        }
    }

    var theme: Theme
        get() {
            val ordinal = sharedPreferences.getInt("theme", Theme.SYSTEM.ordinal)
            return Theme.values()[ordinal]
        }
        set(value) {
            Timber.d("$value in SET called");
            themeLiveData.postValue(value)
            sharedPreferences.edit().putInt("theme", value.ordinal).apply()
        }
}

enum class Theme {
    DARK,
    WHITE,
    SYSTEM
}
