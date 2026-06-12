package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.sirelon.marsroverphotos.domain.settings.Theme
import com.sirelon.marsroverphotos.platform.AppReview
import com.sirelon.marsroverphotos.platform.BuildInfo
import com.sirelon.marsroverphotos.presentation.navigation.AppDestination
import com.sirelon.marsroverphotos.presentation.navigation.LocalAboutCallbacks
import com.sirelon.marsroverphotos.presentation.navigation.LocalAppNavigator
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.activeStatusColor
import com.sirelon.marsroverphotos.presentation.theme.primaryVariant
import com.sirelon.marsroverphotos.presentation.ui.AppBadge
import com.sirelon.marsroverphotos.presentation.ui.AppIconBox
import com.sirelon.marsroverphotos.presentation.ui.AppOutlinedCard
import com.sirelon.marsroverphotos.presentation.ui.BadgeRow
import com.sirelon.marsroverphotos.presentation.ui.CardShape
import com.sirelon.marsroverphotos.presentation.ui.MarsSnackbar
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.SegmentedControl
import com.sirelon.marsroverphotos.presentation.ui.SettingsRow
import com.sirelon.marsroverphotos.presentation.ui.SettingsRowDivider
import com.sirelon.marsroverphotos.presentation.ui.SettingsRowIndent
import com.sirelon.marsroverphotos.presentation.ui.SettingsSectionLabel
import com.sirelon.marsroverphotos.presentation.ui.StatusBadge
import com.sirelon.marsroverphotos.presentation.ui.rememberPlatformUriHandler
import com.sirelon.marsroverphotos.presentation.viewmodels.AboutViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.alien_icon
import kotlin.time.Clock
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AboutScreen() {
    val viewModel: AboutViewModel = koinViewModel()
    val callbacks = LocalAboutCallbacks.current
    val navigator = LocalAppNavigator.current
    val coilContext = LocalPlatformContext.current
    val currentTheme by viewModel.themeFlow.collectAsStateWithLifecycle()
    val showFacts by viewModel.showFactsFlow.collectAsStateWithLifecycle()
    AboutContent(
        currentTheme = currentTheme,
        showFacts = showFacts,
        onThemeChange = viewModel::setTheme,
        onFactsToggle = viewModel::toggleFacts,
        onClearCache = {
            val loader = SingletonImageLoader.get(coilContext)
            loader.diskCache?.clear()
            loader.memoryCache?.clear()
        },
        onRateApp = callbacks.onRateApp,
        appVersion = callbacks.appVersion,
        rateAppUrl = callbacks.rateAppUrl,
        onNavigateToAdmin = if (BuildInfo.isDebug) {
            { navigator.navigate(AppDestination.AdminPhotos) }
        } else null
    )
}

@Composable
private fun AboutContent(
    currentTheme: Theme,
    showFacts: Boolean,
    onThemeChange: (Theme) -> Unit,
    onFactsToggle: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onRateApp: () -> Unit,
    appVersion: String,
    rateAppUrl: String,
    onNavigateToAdmin: (() -> Unit)?
) {
    val uriHandler = rememberPlatformUriHandler()
    val appReview: AppReview = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme
    val live = activeStatusColor()

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero is full-bleed: it sits outside the capped column so its gradient
            // reaches both screen edges on every surface.
            Hero(appVersion = appVersion, live = live)

            // Settings content fills the width (minus the screen padding) until the window
            // reaches the EXPANDED width class — the same adaptive source the navigation uses
            // ([currentWindowAdaptiveInfo]) — beyond which it caps at [AppSize.contentMaxWidth]
            // and centers within the parent column.
            val expandedWidth = currentWindowAdaptiveInfo().windowSizeClass
                .isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
            val contentWidth = if (expandedWidth) {
                Modifier.widthIn(max = AppSize.contentMaxWidth)
            } else {
                Modifier.fillMaxWidth()
            }
            Column(
                modifier = contentWidth
                    .padding(horizontal = AppSpacing.lg)
                    .padding(bottom = AppSpacing.xxl),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)
            ) {
                Blurb()

                SettingsSection(label = "Appearance") {
                    ThemeRow(currentTheme = currentTheme, onThemeChange = onThemeChange)
                    SettingsRowDivider()
                    SettingsRow(
                        icon = MaterialSymbol.Info,
                        iconContainer = colors.secondaryContainer,
                        iconTint = colors.onSecondaryContainer,
                        label = "Fact Cards",
                        sub = if (showFacts) "\"Did You Know?\" cards visible" else "Fact cards hidden",
                        trailing = {
                            Switch(checked = showFacts, onCheckedChange = onFactsToggle)
                        }
                    )
                }

                SettingsSection(label = "Data") {
                    SettingsRow(
                        icon = MaterialSymbol.Delete,
                        iconContainer = colors.error.copy(alpha = 0.16f),
                        iconTint = colors.error,
                        label = "Clear Cache",
                        sub = "Free up storage used by cached photos",
                        onClick = {
                            onClearCache()
                            scope.launch { snackbarHostState.showSnackbar("Cache cleared") }
                        }
                    )
                }

                SettingsSection(label = "Connect") {
                    SettingsRow(
                        icon = MaterialSymbol.Public,
                        iconContainer = live.copy(alpha = 0.15f),
                        iconTint = live,
                        label = "NASA Open API",
                        sub = "api.nasa.gov",
                        onClick = { uriHandler.openUri("https://api.nasa.gov/") }
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        icon = MaterialSymbol.Star,
                        iconContainer = colors.secondary.copy(alpha = 0.18f),
                        iconTint = colors.secondary,
                        label = "Rate the App",
                        sub = "Enjoying Mars? Leave a review",
                        onClick = {
                            scope.launch {
                                val shown = appReview.requestReview()
                                if (!shown) {
                                    if (rateAppUrl.isNotBlank()) {
                                        uriHandler.openUri(rateAppUrl)
                                    } else {
                                        onRateApp()
                                    }
                                }
                            }
                        }
                    )
                    SettingsRowDivider()
                    SettingsRow(
                        icon = MaterialSymbol.Reviews,
                        iconContainer = colors.tertiary.copy(alpha = 0.16f),
                        iconTint = colors.tertiary,
                        label = "Send Feedback",
                        sub = "Report a bug or suggest a feature",
                        onClick = { uriHandler.openUri("mailto:sasha.sirelon@gmail.com") }
                    )
                }

                if (onNavigateToAdmin != null) {
                    SettingsSection(label = "Developer") {
                        SettingsRow(
                            icon = MaterialSymbol.BugReport,
                            iconContainer = colors.errorContainer,
                            iconTint = colors.onErrorContainer,
                            label = "Admin: Popular Photos",
                            sub = "Check and remove stale Firebase photos",
                            onClick = onNavigateToAdmin
                        )
                    }
                }

                Footer()
            }
        }

        MarsSnackbar(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
            actionClick = null
        )
    }
}

/** Section label + grouped card — the About screen's grouped-settings glue. */
@Composable
private fun SettingsSection(label: String, content: @Composable () -> Unit) {
    Column {
        SettingsSectionLabel(text = label)
        AppOutlinedCard { content() }
    }
}

/**
 * Hero gradient top tint — resolved per applied theme so the gradient reads correctly in both.
 * Dark keeps the deep navy [primaryVariant]; light uses a soft light-blue (the design's light
 * variant, ~#DCE8F6) so the hero fades light-blue→white instead of navy→white.
 */
@Composable
private fun heroTopColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) primaryVariant else Color(0xFFDCE8F6)

@Composable
private fun Hero(appVersion: String, live: Color) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(heroTopColor(), colors.background)))
            .padding(horizontal = AppSpacing.xl)
            .padding(top = AppSize.heroTopPadding, bottom = AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Soft coral radial glow
            Box(
                modifier = Modifier
                    .size(AppSize.heroGlow)
                    .background(
                        Brush.radialGradient(listOf(colors.secondary.copy(alpha = 0.20f), Color.Transparent)),
                        shape = CircleShape
                    )
            )
            // Thin coral ring
            Box(
                modifier = Modifier
                    .size(AppSize.heroRing)
                    .border(AppSize.hairline, colors.secondary.copy(alpha = 0.22f), CircleShape)
            )
            Image(
                painter = painterResource(Res.drawable.alien_icon),
                contentDescription = "Mars Rover Photos",
                modifier = Modifier
                    .size(AppSize.heroMascot)
                    .shadow(AppSize.heroShadow, CircleShape)
                    .clip(CircleShape)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Mars Rover Photos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(AppSpacing.xs))
            Text(
                text = "Real NASA rover photography · Red Planet direct",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        BadgeRow {
            AppBadge(text = "v$appVersion")
            StatusBadge(label = "NASA API", color = live)
        }
    }
}

@Composable
private fun Blurb() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.secondaryContainer)
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        MaterialSymbolIcon(
            symbol = MaterialSymbol.Info,
            contentDescription = null,
            tint = colors.onSecondaryContainer,
            size = AppSize.iconInline
        )
        Text(
            text = "Discover Mars with exploration rovers! A great selection of recent and historic " +
                "photos directly from the Red Planet. Maybe you'll even spot signs of Martian life ;-)",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSecondaryContainer
        )
    }
}

/** Theme row: identity line + segmented control on its own line so it fits any width. */
@Composable
private fun ThemeRow(currentTheme: Theme, onThemeChange: (Theme) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpacing.lg, vertical = AppSize.rowVerticalPadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSize.rowIconGap)
        ) {
            AppIconBox(
                symbol = MaterialSymbol.Tune,
                container = colors.tertiary.copy(alpha = 0.16f),
                tint = colors.tertiary
            )
            Column {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface
                )
                Text(
                    text = "Color scheme",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
        // Indent to align under the label (icon-box + gap).
        Row(modifier = Modifier.padding(start = SettingsRowIndent)) {
            SegmentedControl(
                options = listOf(Theme.DARK, Theme.WHITE, Theme.SYSTEM),
                selected = currentTheme,
                onSelect = onThemeChange,
                label = { theme ->
                    when (theme) {
                        Theme.DARK -> "Dark"
                        Theme.WHITE -> "Light"
                        Theme.SYSTEM -> "System"
                    }
                }
            )
        }
    }
}

@Composable
private fun Footer() {
    val colors = MaterialTheme.colorScheme
    val currentYear = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault()).year
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        Text(
            text = "© $currentYear · All rights reserved",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        Text(
            text = "Made with ♥ · Standing with 🇺🇦",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceVariant
        )
    }
}
