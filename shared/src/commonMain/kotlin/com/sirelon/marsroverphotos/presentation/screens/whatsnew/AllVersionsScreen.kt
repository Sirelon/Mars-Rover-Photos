package com.sirelon.marsroverphotos.presentation.screens.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.ReleaseCard
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AllVersionsScreen() {
    val navigator = LocalAppNavigator.current
    val viewModel: WhatsNewViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text("Version History") },
                onBack = { navigator.goBack() },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            state.releases.forEach { release ->
                ReleaseCard(
                    release = release,
                    onOpen = { navigator.navigate(AppDestination.WhatsNewStory(release.version, page = 0)) },
                )
            }
        }
    }
}
