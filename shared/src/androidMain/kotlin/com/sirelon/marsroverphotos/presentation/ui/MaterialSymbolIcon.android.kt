package com.sirelon.marsroverphotos.presentation.ui

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.sirelon.marsroverphotos.shared.R

/**
 * Android implementation: Load Material Symbols font from resources with variable font support.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
actual fun getMaterialSymbolsFont(filled: Boolean, weight: Int): FontFamily {
    return remember(filled, weight) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android O+ supports variable fonts
            FontFamily(
                Font(
                    R.font.material_symbols_outlined,
                    variationSettings = FontVariation.Settings(
                        FontVariation.Setting("FILL", if (filled) 1f else 0f),
                        FontVariation.Setting("wght", weight.toFloat()),
                        FontVariation.Setting("GRAD", 0f),
                        FontVariation.Setting("opsz", 24f)
                    )
                )
            )
        } else {
            // Fallback for older Android versions
            FontFamily(Font(R.font.material_symbols_outlined))
        }
    }
}
