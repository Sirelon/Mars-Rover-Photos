package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.sirelon.marsroverphotos.presentation.viewmodels.PhotosViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.cancel
import com.sirelon.marsroverphotos.shared.resources.choose
import com.sirelon.marsroverphotos.shared.resources.sol_description
import com.sirelon.marsroverphotos.shared.resources.sol_label
import com.sirelon.marsroverphotos.shared.resources.sol_max_error_fmt
import org.jetbrains.compose.resources.stringResource

/**
 * Navigation-entry dialog that lets the user pick a sol number.
 * Shares [PhotosViewModel] with the [com.sirelon.marsroverphotos.presentation.navigation.AppDestination.Photos]
 * back-stack entry via [com.sirelon.marsroverphotos.presentation.navigation.LocalSharedViewModelStoreOwner].
 */
@Composable
fun SolPickerScreen(
    viewModel: PhotosViewModel,
    onDismiss: () -> Unit,
) {
    var maxSol by remember { mutableLongStateOf(1L) }
    LaunchedEffect(Unit) {
        maxSol = viewModel.maxSol().coerceAtLeast(1L)
    }

    val currentSol by viewModel.solFlow.collectAsState(initial = 0L)
    var selectedSol: Long? by remember(currentSol) { mutableStateOf(currentSol) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val maxSolError = stringResource(Res.string.sol_max_error_fmt, maxSol)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val resolved = selectedSol
                    if (resolved != null) {
                        viewModel.loadBySol(resolved)
                    }
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.choose))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        title = { Text(text = stringResource(Res.string.sol_description)) },
        text = {
            SolChanger(
                sol = selectedSol,
                maxSol = maxSol,
                errorMessage = errorMessage,
                onSolChanged = { nextSol ->
                    if ((nextSol ?: 0L) > maxSol) {
                        selectedSol = maxSol
                        errorMessage = maxSolError
                    } else {
                        selectedSol = nextSol
                        errorMessage = null
                    }
                }
            )
        }
    )
}

@Composable
private fun SolChanger(
    sol: Long?,
    maxSol: Long,
    errorMessage: String?,
    onSolChanged: (sol: Long?) -> Unit,
) {
    val sliderMax = maxSol.coerceAtLeast(1L)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.sol_label),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge
            )
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                value = sol?.toString().orEmpty(),
                modifier = Modifier.weight(1f),
                singleLine = true,
                onValueChange = { onSolChanged(it.toLongOrNull()) }
            )
        }
        Slider(
            value = (sol ?: 0L).coerceIn(0L, sliderMax).toFloat(),
            valueRange = 0f..sliderMax.toFloat(),
            onValueChange = { onSolChanged(it.toLong()) }
        )
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
