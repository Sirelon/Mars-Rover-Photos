package com.sirelon.marsroverphotos.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Created on 27.06.2021 13:04 for Mars-Rover-Photos.
 */
@Composable
fun MarsSnackbar(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    actionClick: (() -> Unit)? = null
) {
    val actionText = snackbarHostState.currentSnackbarData?.actionLabel
    SnackbarHost(
        modifier = modifier,
        hostState = snackbarHostState,
        snackbar = {
            Snackbar(
                action = {
                    if (actionText != null && actionClick != null)
                        Button(onClick = {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            actionClick()
                        }) {
                            Text(text = actionText)
                        }
                },
                content = { Text(text = snackbarHostState.currentSnackbarData?.message ?: "") },
            )
        })
}