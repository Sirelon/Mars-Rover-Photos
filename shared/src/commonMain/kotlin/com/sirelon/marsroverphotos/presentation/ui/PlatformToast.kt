package com.sirelon.marsroverphotos.presentation.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Cross-platform toast replacement built on Material3 snackbars.
 */
@Stable
class PlatformToastState internal constructor(
    val snackbarHostState: SnackbarHostState
) {
    suspend fun showToast(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null
    ) {
        val result = snackbarHostState.showSnackbar(
            message = message,
            actionLabel = actionLabel,
            duration = duration
        )
        if (result == SnackbarResult.ActionPerformed) {
            onAction?.invoke()
        }
    }
}

@Composable
fun rememberPlatformToastState(): PlatformToastState {
    val snackbarHostState = remember { SnackbarHostState() }
    return remember(snackbarHostState) {
        PlatformToastState(snackbarHostState)
    }
}

@Composable
fun PlatformToastHost(
    state: PlatformToastState,
    modifier: Modifier = Modifier
) {
    MarsSnackbar(
        modifier = modifier,
        snackbarHostState = state.snackbarHostState,
        actionClick = null
    )
}
