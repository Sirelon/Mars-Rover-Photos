package com.sirelon.marsroverphotos.widget

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sirelon.marsroverphotos.feature.rovers.CuriosityId
import com.sirelon.marsroverphotos.feature.rovers.InsightId
import com.sirelon.marsroverphotos.feature.rovers.OpportunityId
import com.sirelon.marsroverphotos.feature.rovers.PerserveranceId
import com.sirelon.marsroverphotos.feature.rovers.SpiritId
import com.sirelon.marsroverphotos.feature.rovers.RoversActivity
import java.io.File

internal const val WidgetExtraImageId = "com.sirelon.marsroverphotos.widget.EXTRA_IMAGE_ID"

internal object MarsPhotoWidgetState {
    val roverIdKey = longPreferencesKey("mars_widget_rover_id")
    val imagePathKey = stringPreferencesKey("mars_widget_image_path")
    val imageIdKey = stringPreferencesKey("mars_widget_image_id")
    val roverNameKey = stringPreferencesKey("mars_widget_rover_name")
    val solKey = longPreferencesKey("mars_widget_sol")
    val earthDateKey = stringPreferencesKey("mars_widget_earth_date")
}

internal const val DefaultRoverId = CuriosityId

public class MarsPhotoWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 140.dp),
            DpSize(200.dp, 140.dp),
            DpSize(260.dp, 200.dp)
        )
    )

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val state = currentState<Preferences>()
        val configuredRoverId = state[MarsPhotoWidgetState.roverIdKey]
        val roverId = configuredRoverId ?: DefaultRoverId
        val hasConfiguredRover = configuredRoverId != null
        val roverName = state[MarsPhotoWidgetState.roverNameKey] ?: roverNameForId(roverId)
        val sol = state[MarsPhotoWidgetState.solKey] ?: 0L
        val earthDate = state[MarsPhotoWidgetState.earthDateKey].orEmpty()
        val imagePath = state[MarsPhotoWidgetState.imagePathKey]
        val imageId = state[MarsPhotoWidgetState.imageIdKey]
        val size = LocalSize.current

        val imageProvider = imagePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let { bitmap ->
                    ImageProvider(bitmap)
                }
            } else {
                null
            }
        }

        val launchIntent = Intent(context, RoversActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!imageId.isNullOrBlank()) {
                putExtra(WidgetExtraImageId, imageId)
            }
        }

        val action = actionStartActivity(launchIntent)
        val showDetails = size.width >= 200.dp && size.height >= 140.dp
        val showOverlay = size.height >= 110.dp

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(action)
        ) {
            if (imageProvider != null) {
                Image(
                    provider = imageProvider,
                    contentDescription = roverName,
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF1A283D))
                ) {
                    Text(
                        text = if (hasConfiguredRover) "Updating photo" else "Select a rover",
                        modifier = GlanceModifier
                            .padding(12.dp)
                            .align(Alignment.Center),
//                        style = TextStyle(
//                            color = Color.White,
//                            fontWeight = FontWeight.Bold,
//                            fontSize = 14.sp
//                        )
                    )
                }
            }

            if (showOverlay && imageProvider != null) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(ColorProvider(androidx.compose.ui.graphics.Color(0xAA000000)))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = roverName,
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                    if (showDetails) {
                        val detail = if (sol > 0 && earthDate.isNotBlank()) {
                            "Sol $sol - $earthDate"
                        } else if (earthDate.isNotBlank()) {
                            earthDate
                        } else if (sol > 0) {
                            "Sol $sol"
                        } else {
                            ""
                        }
                        if (detail.isNotBlank()) {
                            Text(
                                text = detail,
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFE0E0E0)),
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun roverNameForId(roverId: Long): String {
    return when (roverId) {
        PerserveranceId -> "Perseverance"
        InsightId -> "Insight"
        OpportunityId -> "Opportunity"
        SpiritId -> "Spirit"
        CuriosityId -> "Curiosity"
        else -> "Mars Rover"
    }
}
