package com.sirelon.marsroverphotos.ui

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Created on 27.06.2021 13:04 for Mars-Rover-Photos.
 */
@Composable
fun MarsSnackbar(
    modifier: Modifier = Modifier,
    text: String,
    actionText: String? = null,
    actionClick: (() -> Unit)? = null
) {
    val snackbarHostState = remember { mutableStateOf(SnackbarHostState()) }

    LaunchedEffect(key1 = text, block = {
        snackbarHostState.value.showSnackbar(message = text, actionLabel = actionText)
    })

    SnackbarHost(
        modifier = modifier,
        hostState = snackbarHostState.value,
        snackbar = {
            Snackbar(
                action = {
                    if (actionText != null && actionClick != null)
                        Button(onClick = {
                            snackbarHostState.value.currentSnackbarData?.dismiss()
                            actionClick()
                        }) {
                            Text(text = actionText)
                        }
                },
                content = { Text(text = text) },
            )
        })
}