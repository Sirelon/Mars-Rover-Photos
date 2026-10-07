package com.sirelon.marsroverphotos.presentation.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import com.sirelon.marsroverphotos.domain.support.SupportTier
import com.sirelon.marsroverphotos.domain.support.SupportTierKind
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.ui.AppButton
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppIconBox
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedButton
import com.sirelon.marsroverphotos.presentation.ui.AppRow
import com.sirelon.marsroverphotos.presentation.ui.AppRowDivider
import com.sirelon.marsroverphotos.presentation.ui.AppSection
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.ui.CenteredProgress
import com.sirelon.marsroverphotos.presentation.ui.MarsSnackbar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.viewmodels.SupportUiEvent
import com.sirelon.marsroverphotos.presentation.viewmodels.SupportUiState
import com.sirelon.marsroverphotos.presentation.viewmodels.SupportViewModel
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.viewmodel.koinViewModel

/**
 * "Support the developer": the purchasable tiers, the ad-free status and Restore.
 *
 * Wording deliberately says tip / support, never "donation" — both stores treat in-app donations
 * as a different, restricted category (Apple 3.2.2, Play payments policy).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen() {
    val navigator = LocalAppNavigator.current
    val viewModel: SupportViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.load()
        viewModel.events.collect { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    SupportUiEvent.PurchaseSucceeded -> "Thank you! Ads are gone."
                    SupportUiEvent.TipThanks -> "Thanks for the coffee! Ads are off for 30 days."
                    SupportUiEvent.Restored -> "Purchases restored."
                    SupportUiEvent.NothingToRestore -> "No earlier purchase found on this store account."
                    // Already a full sentence from the repository ("Nothing was charged — …").
                    is SupportUiEvent.Failed -> event.message
                }
            )
        }
    }

    // Cap+center only in the EXPANDED width class, the same adaptive source the nav suite uses.
    val expandedWidth = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val contentWidth = if (expandedWidth) Modifier.widthIn(max = AppSize.contentMaxWidth) else Modifier.fillMaxWidth()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(),
        topBar = {
            AppTopBar(
                scrollBehavior = scrollBehavior,
                title = { Text("Support the Developer") },
                onBack = dropUnlessResumed { navigator.goBack() },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> CenteredProgress()

                // The repository returns an empty list when the store cannot be reached: a normal
                // state with a retry, not an error dialog.
                state.tiers.isEmpty() -> AppEmptyState(
                    title = "The store isn't reachable right now",
                    action = { AppButton(onClick = viewModel::retry) { Text("Try again") } },
                )

                else -> SupportContent(
                    state = state,
                    contentWidth = contentWidth,
                    onPurchase = viewModel::purchase,
                    onRestore = viewModel::restore,
                )
            }

            MarsSnackbar(
                snackbarHostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SupportContent(
    state: SupportUiState,
    contentWidth: Modifier,
    onPurchase: (String) -> Unit,
    onRestore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = contentWidth
                .padding(horizontal = AppSpacing.lg)
                .padding(bottom = AppSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl),
        ) {
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Intro()
            if (state.isAdFree) SupporterCard()
            TierList(
                tiers = state.tiers,
                purchasingTierId = state.purchasingTierId,
                enabled = !state.isBusy,
                onPurchase = onPurchase,
            )
            RestoreRow(isRestoring = state.isRestoring, enabled = !state.isBusy, onRestore = onRestore)
        }
    }
}

@Composable
private fun Intro() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        AppIconBox(
            symbol = MaterialSymbol.VolunteerActivism,
            container = colors.tertiaryContainer,
            tint = colors.onTertiaryContainer,
            size = AppSize.iconBoxHero,
            iconSize = AppSize.iconDefault,
        )
        Text(
            text = "Mars Rover Photos is free and built by one person. A coffee switches ads off for 30 days, " +
                "any time you like. Lunch or more switches them off for good, and Restore brings that back.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SupporterCard() {
    val colors = MaterialTheme.colorScheme
    AppSection {
        AppRow(
            icon = MaterialSymbol.CheckCircle,
            iconContainer = colors.primaryContainer,
            iconTint = colors.onPrimaryContainer,
            label = "You're a supporter — thank you!",
            sub = "Ads are off. Thank you for keeping the rovers rolling.",
        )
    }
}

@Composable
private fun TierList(
    tiers: ImmutableList<SupportTier>,
    purchasingTierId: String?,
    enabled: Boolean,
    onPurchase: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppSection(label = "Pick a tier") {
        tiers.forEachIndexed { index, tier ->
            if (index > 0) AppRowDivider()
            AppRow(
                icon = tier.symbol(),
                iconContainer = colors.secondaryContainer,
                iconTint = colors.onSecondaryContainer,
                label = tier.title,
                sub = when (tier.kind) {
                    SupportTierKind.TIP -> "Removes ads for 30 days · tip any time"
                    SupportTierKind.AD_FREE -> "Removes ads forever"
                },
                onClick = if (enabled) ({ onPurchase(tier.id) }) else null,
                trailing = {
                    if (purchasingTierId == tier.id) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AppSize.icon),
                            strokeWidth = AppSize.inlineProgressStroke,
                        )
                    } else {
                        Text(
                            text = tier.price,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun RestoreRow(isRestoring: Boolean, enabled: Boolean, onRestore: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        AppOutlinedButton(onClick = onRestore, enabled = enabled) {
            if (isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(AppSize.iconInline),
                    strokeWidth = AppSize.inlineProgressStroke,
                )
            } else {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(AppSize.iconInline),
                )
            }
            Spacer(modifier = Modifier.size(AppSpacing.sm))
            Text("Restore purchases")
        }
        Text(
            text = "Bought Lunch or more on another device? Restore picks it up.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Package ids are the RevenueCat package lookup keys; they outlive store product renames. */
private fun SupportTier.symbol(): MaterialSymbol = when (packageId) {
    "support_small" -> MaterialSymbol.Coffee
    "support_medium" -> MaterialSymbol.LunchDining
    "support_large" -> MaterialSymbol.DinnerDining
    "support_huge" -> MaterialSymbol.Celebration
    else -> MaterialSymbol.Favorite
}
