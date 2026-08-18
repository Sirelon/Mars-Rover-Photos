package com.sirelon.marsroverphotos.presentation.screens.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllVersionsScreen() {
    val navigator = LocalAppNavigator.current
    val viewModel: WhatsNewViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Cap+center the release list only in the EXPANDED width class — the same adaptive source the
    // nav suite uses (see docs/DESIGN_SYSTEM.md › one source, two breakpoints).
    val expandedWidth = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val contentWidth = if (expandedWidth) {
        Modifier.widthIn(max = AppSize.contentMaxWidth)
    } else {
        Modifier.fillMaxWidth()
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(),
        topBar = {
            AppTopBar(
                scrollBehavior = scrollBehavior,
                title = { Text("Version History") },
                onBack = { navigator.goBack() },
            )
        },
    ) { innerPadding ->
        // padding before verticalScroll: the top-bar inset is a fixed viewport edge, so it must not
        // scroll away underneath the bar with the content.
        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                CenteredProgress()
            }

            // The notes come from Firestore, so an empty list means the fetch failed or nothing is
            // published yet — neither is an error worth a dialog, but the screen must say something.
            state.releases.isEmpty() -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AppEmptyState(title = "No release notes yet")
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = contentWidth
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
    }
}
