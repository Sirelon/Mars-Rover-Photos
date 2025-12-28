package com.sirelon.marsroverphotos.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.sirelon.marsroverphotos.feature.rovers.CuriosityId
import com.sirelon.marsroverphotos.feature.rovers.InsightId
import com.sirelon.marsroverphotos.feature.rovers.OpportunityId
import com.sirelon.marsroverphotos.feature.rovers.PerserveranceId
import com.sirelon.marsroverphotos.feature.rovers.SpiritId
import com.sirelon.marsroverphotos.ui.MarsRoverPhotosTheme
import kotlinx.coroutines.launch

public class MarsPhotoWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setResult(Activity.RESULT_CANCELED)

        appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MarsRoverPhotosTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    onConfirm = { roverId ->
                        saveSelection(roverId)
                    }
                )
            }
        }
    }

    private fun saveSelection(roverId: Long) {
        lifecycleScope.launch {
            val manager = GlanceAppWidgetManager(this@MarsPhotoWidgetConfigActivity)
            val glanceId = manager.getGlanceIdBy(appWidgetId)

            updateAppWidgetState(this@MarsPhotoWidgetConfigActivity, glanceId) { prefs ->
                prefs[MarsPhotoWidgetState.roverIdKey] = roverId
                prefs[MarsPhotoWidgetState.roverNameKey] = roverNameForId(roverId)
                prefs.remove(MarsPhotoWidgetState.imagePathKey)
                prefs.remove(MarsPhotoWidgetState.imageIdKey)
                prefs.remove(MarsPhotoWidgetState.solKey)
                prefs.remove(MarsPhotoWidgetState.earthDateKey)
            }

            MarsPhotoWidget().update(this@MarsPhotoWidgetConfigActivity, glanceId)
            MarsPhotoWidgetWorker.enqueueOnce(this@MarsPhotoWidgetConfigActivity)
            MarsPhotoWidgetWorker.enqueuePeriodic(this@MarsPhotoWidgetConfigActivity)

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WidgetConfigScreen(
    appWidgetId: Int,
    onConfirm: (Long) -> Unit
) {
    val context = LocalContext.current
    val roverOptions = remember {
        listOf(
            RoverOption(PerserveranceId, "Perseverance"),
            RoverOption(CuriosityId, "Curiosity"),
            RoverOption(OpportunityId, "Opportunity"),
            RoverOption(SpiritId, "Spirit"),
            RoverOption(InsightId, "Insight")
        )
    }
    var selectedId by remember { mutableStateOf(DefaultRoverId) }

    LaunchedEffect(appWidgetId) {
        val manager = GlanceAppWidgetManager(context)
        val glanceId = manager.getGlanceIdBy(appWidgetId)
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
        selectedId = prefs[MarsPhotoWidgetState.roverIdKey] ?: DefaultRoverId
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Choose rover") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                items(roverOptions) { option ->
                    RoverRow(
                        option = option,
                        selected = option.id == selectedId,
                        onClick = { selectedId = option.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onConfirm(selectedId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Add widget")
            }
        }
    }
}

@Composable
private fun RoverRow(
    option: RoverOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = option.name, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Latest photo", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private data class RoverOption(
    val id: Long,
    val name: String
)
