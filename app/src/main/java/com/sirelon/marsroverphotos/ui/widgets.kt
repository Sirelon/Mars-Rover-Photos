package com.sirelon.marsroverphotos.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Created on 27.06.2021 13:04 for Mars-Rover-Photos.
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
                    if (actionText != null && actionClick != null)
                        Button(onClick = {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            actionClick()
                        }) {
                            Text(text = actionText)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoScrollEffect(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        content()
    }
}
