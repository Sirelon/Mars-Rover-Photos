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
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing

/**
 * Design-system snackbar with an optional action button.
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
                    if (actionText != null) {
                        Button(onClick = {
                            snackbarHostState.currentSnackbarData?.performAction()
                            actionClick?.invoke()
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
 * Radio button with a composable label slot.
 *
 * @param selected Whether the radio button is selected
 * @param onClick Callback when the component is clicked
 * @param modifier Modifier for the component
 * @param labelContent The label rendered below the radio button
 */
@Composable
fun RadioButtonText(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(all = AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        RadioButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            selected = selected,
            onClick = null
        )
        labelContent()
    }
}

/**
 * Disables scroll overscroll effect (glow on Android, bounce on iOS).
 * Useful for custom scroll implementations or when overscroll is not desired.
 *
 * @param content The content to display without overscroll effect
 */
@Composable
fun NoScrollEffect(content: @Composable () -> Unit) {
    content()
}
