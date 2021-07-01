package com.sirelon.marsroverphotos.ui

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

/**
 * Created on 27.06.2021 13:04 for Mars-Rover-Photos.
 */
@Composable
fun Snackbar(text: String, actionText: String? = null, actionClick: (() -> Unit)? = null) {
    androidx.compose.material.Snackbar(action = {
        if (actionText != null && actionClick != null)
            Button(onClick = actionClick) {
                Text(text = actionText)
            }
    }, content = { Text(text = text) })
}