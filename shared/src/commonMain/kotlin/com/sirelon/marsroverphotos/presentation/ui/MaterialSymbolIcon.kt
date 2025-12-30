package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sirelon.marsroverphotos.shared.resources.Res
import kotlin.math.max
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font as ResourceFont

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
    Error("error"),
    CheckCircle("check_circle")
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
 * @param filled Whether to show the filled variant (true) or outlined (false).
 *               On platforms without font-variation support, this falls back to a heavier weight.
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
    val fontFamily = materialSymbolsFontFamily(filled = filled, weight = weight)

    // Convert size to sp for font size
    val fontSize = (size.value * 1.5).sp // Adjust multiplier as needed for proper sizing

    Text(
        text = symbol.iconName,
        modifier = modifier.size(size),
        color = tint,
        fontFamily = fontFamily,
        fontSize = fontSize,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

/**
 * Resolve the Material Symbols font family from Compose resources.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
private fun materialSymbolsFontFamily(
    filled: Boolean,
    weight: Int
): FontFamily {
    val resolvedWeight = if (filled) max(weight, 600) else weight
    val fontWeight = FontWeight(resolvedWeight.coerceIn(100, 700))

    return FontFamily(
        ResourceFont(
            Res.font.material_symbols_outlined,
            weight = fontWeight
        )
    )
}
