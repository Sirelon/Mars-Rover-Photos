package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.rememberCoroutineScope
import com.sirelon.marsroverphotos.platform.AppUpdate
import com.sirelon.marsroverphotos.presentation.ui.AppCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.sirelon.marsroverphotos.domain.models.Rover
import com.sirelon.marsroverphotos.presentation.theme.AppSize
import com.sirelon.marsroverphotos.presentation.theme.AppSpacing
import com.sirelon.marsroverphotos.presentation.theme.activeStatusColor
import com.sirelon.marsroverphotos.presentation.ui.AppEmptyState
import com.sirelon.marsroverphotos.presentation.ui.AppMetricItem
import com.sirelon.marsroverphotos.presentation.ui.AppTopBar
import com.sirelon.marsroverphotos.presentation.navigation.LocalAboutCallbacks
import com.sirelon.marsroverphotos.presentation.ui.AppNoticeCard
import com.sirelon.marsroverphotos.presentation.ui.rememberPlatformUriHandler
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewCard
import com.sirelon.marsroverphotos.presentation.viewmodels.WhatsNewViewModel
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbol
import com.sirelon.marsroverphotos.presentation.ui.MaterialSymbolIcon
import com.sirelon.marsroverphotos.presentation.ui.StatusBadge
import com.sirelon.marsroverphotos.presentation.ui.blurb
import com.sirelon.marsroverphotos.presentation.ui.painter
import com.sirelon.marsroverphotos.presentation.viewmodels.RoversViewModel
import com.sirelon.marsroverphotos.shared.resources.Res
import com.sirelon.marsroverphotos.shared.resources.metric_label_last
import com.sirelon.marsroverphotos.shared.resources.metric_label_photos
import com.sirelon.marsroverphotos.shared.resources.metric_label_sols
import com.sirelon.marsroverphotos.shared.resources.rovers_search_empty_fmt
import com.sirelon.marsroverphotos.shared.resources.rovers_search_placeholder
import com.sirelon.marsroverphotos.shared.resources.rovers_subtitle_fmt
import com.sirelon.marsroverphotos.shared.resources.rovers_title
import com.sirelon.marsroverphotos.utils.formatCompact
import com.sirelon.marsroverphotos.utils.formatDisplayDate
import com.sirelon.marsroverphotos.utils.nasaImageSmallFallbackUrls
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private const val WHATS_NEW_CARD_KEY = "whats-new-card"

@Composable
fun RoversScreen(
    onNavigateToPhotos: (Long) -> Unit,
    onMissionInfoClick: (Long) -> Unit,
    onOpenWhatsNewStory: (version: String) -> Unit,
) {
    val viewModel: RoversViewModel = koinViewModel()
    val rovers by viewModel.rovers.collectAsStateWithLifecycle()
    val filteredRovers by viewModel.filteredRovers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val coverUrls by viewModel.coverUrls.collectAsStateWithLifecycle()

    // The release card lives here, at the top of the list, rather than as a launch dialog: the
    // decision of which card (if any) is the ViewModel's, this screen only renders it.
    val whatsNewViewModel: WhatsNewViewModel = koinViewModel()
    val whatsNewState by whatsNewViewModel.state.collectAsStateWithLifecycle()
    val whatsNewCard = whatsNewState.card
    val storeUrl = LocalAboutCallbacks.current.rateAppUrl
    val uriHandler = rememberPlatformUriHandler()
    val appUpdate: AppUpdate = koinInject()
    val scope = rememberCoroutineScope()
    LaunchedEffect(whatsNewCard) {
        if (whatsNewCard != null) whatsNewViewModel.onCardShown(whatsNewCard)
    }

    RoversContent(
        rovers = filteredRovers,
        allRovers = rovers,
        coverUrls = coverUrls,
        searchQuery = searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onClick = { rover ->
            viewModel.onRoverClicked(rover)
            onNavigateToPhotos(rover.id)
        },
        onMissionInfoClick = { rover ->
            viewModel.onMissionInfoClicked(rover)
            onMissionInfoClick(rover.id)
        },
        whatsNewCard = whatsNewCard,
        onWhatsNewCardClick = { card ->
            whatsNewViewModel.onCardOpened(card)
            when (card) {
                // Android runs Play's own update screen; anywhere it is not on offer, the store opens.
                is WhatsNewCard.UpdateAvailable -> scope.launch {
                    if (!appUpdate.startUpdate() && storeUrl.isNotBlank()) uriHandler.openUri(storeUrl)
                }
                is WhatsNewCard.Highlights -> onOpenWhatsNewStory(card.release.version)
            }
        },
        onWhatsNewCardDismiss = whatsNewViewModel::onCardDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoversContent(
    rovers: List<Rover>,
    allRovers: List<Rover>,
    coverUrls: Map<Long, String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit,
    modifier: Modifier = Modifier,
    whatsNewCard: WhatsNewCard? = null,
    onWhatsNewCardClick: (WhatsNewCard) -> Unit = {},
    onWhatsNewCardDismiss: (WhatsNewCard) -> Unit = {},
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    // 1 column on compact, 2 on medium AND expanded — same adaptive source as the nav suite.
    val columns = if (windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)) 2 else 1
    // Cap+center the whole content column only in the EXPANDED width class (the AboutScreen pattern).
    val expandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    val contentWidth = if (expandedWidth) {
        Modifier.widthIn(max = AppSize.contentMaxWidth)
    } else {
        Modifier.fillMaxWidth()
    }

    val colors = MaterialTheme.colorScheme
    val activeCount = allRovers.count { it.status.equals("active", ignoreCase = true) }

    // Search stays hidden behind the top-bar search action until the user opts in. The flag is
    // saveable so it survives recreation, and search mode also stays visible whenever a query is
    // active — the VM-held query outlives this composable, so the field + clear action must never
    // disappear while filtering a non-empty query.
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val showSearch = searchActive || searchQuery.isNotBlank()
    val focusRequester = remember { FocusRequester() }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(),
        topBar = {
            AppTopBar(
                title = {
                    if (showSearch) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            leadingIcon = {
                                MaterialSymbolIcon(
                                    symbol = MaterialSymbol.Search,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant
                                )
                            },
                            placeholder = { Text(stringResource(Res.string.rovers_search_placeholder)) }
                        )
                    } else {
                        Text(text = stringResource(Res.string.rovers_title))
                    }
                },
                subtitle = if (showSearch) {
                    null
                } else {
                    {
                        Text(
                            text = stringResource(
                                Res.string.rovers_subtitle_fmt,
                                allRovers.size,
                                activeCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    if (showSearch) {
                        IconButton(onClick = {
                            searchActive = false
                            onSearchQueryChange("")
                        }) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.Close,
                                contentDescription = "Close search"
                            )
                        }
                    } else {
                        IconButton(onClick = { searchActive = true }) {
                            MaterialSymbolIcon(
                                symbol = MaterialSymbol.Search,
                                contentDescription = "Search"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { innerPadding ->
        // Focus the field the moment search mode turns on so the keyboard appears immediately.
        LaunchedEffect(searchActive) {
            if (searchActive) focusRequester.requestFocus()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (rovers.isEmpty() && searchQuery.isNotBlank()) {
                AppEmptyState(
                    title = stringResource(Res.string.rovers_search_empty_fmt, searchQuery),
                    showImage = false,
                    modifier = contentWidth
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = contentWidth.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = AppSpacing.md,
                        end = AppSpacing.md,
                        top = innerPadding.calculateTopPadding() + AppSpacing.sm,
                        bottom = innerPadding.calculateBottomPadding() + AppSpacing.sm
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    if (whatsNewCard != null) {
                        item(key = WHATS_NEW_CARD_KEY, span = { GridItemSpan(maxLineSpan) }) {
                            AppNoticeCard(
                                icon = whatsNewCard.noticeIcon(),
                                title = whatsNewCard.noticeTitle(),
                                sub = whatsNewCard.noticeSub(),
                                onClick = { onWhatsNewCardClick(whatsNewCard) },
                                onDismiss = { onWhatsNewCardDismiss(whatsNewCard) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    items(rovers, key = { it.id }) { item ->
                        RoverItem(
                            modifier = Modifier.fillMaxWidth(),
                            rover = item,
                            photoUrl = coverUrls[item.id],
                            onClick = onClick,
                            onMissionInfoClick = onMissionInfoClick
                        )
                    }
                }
            }
        }
    }
}

private fun WhatsNewCard.noticeIcon(): MaterialSymbol = when (this) {
    is WhatsNewCard.UpdateAvailable -> MaterialSymbol.NewReleases
    is WhatsNewCard.Highlights -> MaterialSymbol.Rocket
}

private fun WhatsNewCard.noticeTitle(): String = when (this) {
    is WhatsNewCard.UpdateAvailable -> "Update to ${release.version}"
    is WhatsNewCard.Highlights -> "What's new in ${release.version}"
}

/** The first two change titles, as a one-line teaser of what the tap leads to. */
private fun WhatsNewCard.noticeSub(): String? =
    release.changes.take(2).joinToString(" · ") { it.title }.takeIf { it.isNotBlank() }

@Composable
fun RoverItem(
    rover: Rover,
    onClick: (rover: Rover) -> Unit,
    onMissionInfoClick: (rover: Rover) -> Unit,
    modifier: Modifier = Modifier,
    photoUrl: String? = null,
) {
    AppCard(
        modifier = modifier.testTag("roverCard"),
        onClick = { onClick(rover) },
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(AppSpacing.md),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                RoverThumbnail(
                    rover = rover,
                    photoUrl = photoUrl,
                    modifier = Modifier
                        .width(AppSize.roverThumbWidth)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.large)
                )
                Column(modifier = Modifier.weight(1f)) {
                    TitleLine(rover)
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Text(
                        text = rover.blurb(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    HorizontalDivider(
                        thickness = AppSize.hairline,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    MetricStrip(rover)
                }
            }
            IconButton(
                onClick = { onMissionInfoClick(rover) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                MaterialSymbolIcon(
                    symbol = MaterialSymbol.Info,
                    contentDescription = "Mission Info",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * The rover's static artwork, with its latest real photo crossfaded over it once that loads.
 *
 * The artwork is drawn first and always: it is the image while the photo loads, and it stays the
 * image when the photo fails or never arrives — there is no error state. It also stays the child
 * that sizes the box, because the row measures at `IntrinsicSize.Min`: the photo fills the box
 * with `matchParentSize()`, so a tall Viking frame cannot stretch the card.
 *
 * When [photoUrl] changes (a newer photo on refresh), the previous photo stays up as the
 * placeholder until the new one has loaded, so the card never flashes back to the artwork.
 */
@Composable
private fun RoverThumbnail(
    rover: Rover,
    photoUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = rover.painter(),
            contentDescription = rover.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (photoUrl != null) {
            val context = LocalPlatformContext.current
            // Spirit/Opportunity ~small URLs can 403; walk the same chain as the photo grid. Any
            // other URL is a one-element chain.
            val chain = remember(photoUrl) { nasaImageSmallFallbackUrls(photoUrl) }
            var attempt by remember(photoUrl) { mutableIntStateOf(0) }
            var shownPhoto by remember { mutableStateOf<Painter?>(null) }
            // A fixed request size, rather than one Coil resolves after layout, lets Coil answer
            // from its memory cache in the first frame. Without it a card scrolled back into view
            // shows the artwork for a frame or two and then crossfades to the photo again. Twice
            // the width covers the portrait box, so a cropped landscape frame stays sharp.
            val sizePx = with(LocalDensity.current) { AppSize.roverThumbWidth.roundToPx() }
            val request = remember(chain, attempt, sizePx) {
                ImageRequest.Builder(context)
                    .data(chain[attempt])
                    .size(width = sizePx, height = sizePx * 2)
                    .crossfade(true)
                    .build()
            }
            AsyncImage(
                model = request,
                // Decorative: the artwork underneath already names the rover.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = shownPhoto,
                error = shownPhoto,
                onSuccess = { shownPhoto = it.painter },
                onError = { if (attempt < chain.lastIndex) attempt += 1 },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleLine(rover: Rover) {
    val active = rover.status.equals("active", ignoreCase = true)
    // FlowRow so a long rover name + status chip wrap gracefully instead of truncating tight.
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = AppSize.roverInfoReserve),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        Text(
            text = rover.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        StatusBadge(
            label = if (active) "Active" else "Complete",
            color = if (active) activeStatusColor() else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricStrip(rover: Rover) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        AppMetricItem(
            symbol = MaterialSymbol.Collections,
            value = formatCompact(rover.totalPhotos),
            label = stringResource(Res.string.metric_label_photos)
        )
        AppMetricItem(
            symbol = MaterialSymbol.Schedule,
            value = formatCompact(rover.maxSol),
            label = stringResource(Res.string.metric_label_sols)
        )
        AppMetricItem(
            symbol = MaterialSymbol.Event,
            value = formatDisplayDate(rover.maxDate),
            label = stringResource(Res.string.metric_label_last)
        )
    }
}
