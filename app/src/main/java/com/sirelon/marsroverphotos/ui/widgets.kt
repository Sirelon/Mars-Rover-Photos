package com.sirelon.marsroverphotos.ui

import androidx.compose.material.Button
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
    Snackbar(
        action = {
            if (actionText != null && actionClick != null)
                Button(onClick = actionClick) {
                    Text(text = actionText)
                }
        },
        content = { Text(text = text) },
        modifier = modifier,
    )
}