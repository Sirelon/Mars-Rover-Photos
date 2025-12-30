package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Custom Snackbar component for Mars Rover Photos app.
 * Displays messages with optional action button.
 *
 * @param modifier Modifier for the snackbar host
 * @param snackbarHostState State controlling the snackbar
 * @param actionClick Optional callback when action button is clicked
 */
@Composable
fun MarsSnackbar(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    actionClick: (() -> Unit)? = null
) {
    val actionText = snackbarHostState.currentSnackbarData?.visuals?.actionLabel
    SnackbarHost(
        modifier = modifier,
        hostState = snackbarHostState,
        snackbar = {
            Snackbar(
                action = {
                    if (actionText != null && actionClick != null) {
                        Button(onClick = {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            actionClick()
                        }) {
                            Text(text = actionText)
                        }
                    }
                },
                content = {
                    Text(
                        text = snackbarHostState.currentSnackbarData?.visuals?.message ?: ""
                    )
                },
            )
        })
}

/**
 * Radio button with text label component.
 *
 * @param text The label text to display
 * @param selected Whether the radio button is selected
 * @param modifier Modifier for the component
 * @param onClick Callback when the component is clicked
 */
@Composable
fun RadioButtonText(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            selected = selected,
            onClick = null
        )
        Text(text = text)
    }
}

/**
 * Disables scroll overscroll effect (glow on Android, bounce on iOS).
 * Useful for custom scroll implementations or when overscroll is not desired.
 *
 * Platform-specific implementation via expect/actual.
 *
 * @param content The content to display without overscroll effect
 */
@Composable
expect fun NoScrollEffect(content: @Composable () -> Unit)
