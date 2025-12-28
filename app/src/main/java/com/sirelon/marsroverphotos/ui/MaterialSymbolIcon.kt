package com.sirelon.marsroverphotos.ui

import android.os.Build
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sirelon.marsroverphotos.R

/**
 * Material Symbols icon names used in the app.
 * These names correspond to ligatures in the Material Symbols font.
 *
 * @see <a href="https://fonts.google.com/icons">Material Symbols</a>
 */
enum class MaterialSymbol(val iconName: String) {
    Favorite("favorite"),
    Save("save"),
    Share("share"),
    Visibility("visibility"),
    ZoomIn("zoom_in"),
    Autorenew("autorenew"),
    Info("info"),
    LocalFireDepartment("local_fire_department"),
    ViewCarousel("view_carousel"),
    ViewList("view_list"),
    GridView("grid_view"),
    ArrowBack("arrow_back"),
    Rocket("rocket_launch"),
    FlightLand("flight_land"),
    Star("star"),
    Flag("flag"),
    CameraAlt("photo_camera"),
    Error("error")
}

/**
 * Displays a Material Symbol icon using the Material Symbols font.
 *
 * The icon is rendered using font ligatures - the [symbol] name is converted
 * to the corresponding icon glyph by the font.
 *
 * @param symbol The icon to display from [MaterialSymbol] enum
 * @param contentDescription Accessibility description for the icon
 * @param modifier Modifier to be applied to the icon
 * @param tint Color to tint the icon
 * @param size Size of the icon (default 24.dp)
 * @param filled Whether to show the filled variant (true) or outlined (false)
 * @param weight Font weight from 100 to 700 (default 400)
 */
@Composable
fun MaterialSymbolIcon(
    symbol: MaterialSymbol,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
    filled: Boolean = false,
    weight: Int = 400
) {
    val fontFamily = rememberMaterialSymbolsFont(filled = filled, weight = weight)
    val fontSize = with(LocalDensity.current) { size.toSp() }

    androidx.compose.material3.Text(
        text = symbol.iconName,
        modifier = modifier.size(size),
        color = tint,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberMaterialSymbolsFont(
    filled: Boolean,
    weight: Int
): FontFamily {
    return remember(filled, weight) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            // Fallback for API < 26 - variable font settings not supported
            FontFamily(Font(R.font.material_symbols_outlined))
        }
    }
}
