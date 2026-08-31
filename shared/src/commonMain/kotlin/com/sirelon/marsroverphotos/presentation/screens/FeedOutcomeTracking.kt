package com.sirelon.marsroverphotos.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.sirelon.marsroverphotos.data.paging.usesPageFeed
import com.sirelon.marsroverphotos.platform.Tracker
import org.koin.compose.koinInject

/**
 * Reports a paged feed that reached the user with nothing in it.
 *
 * Only *settled* states count, so the first load never reports itself as a failure while it is
 * still in flight: a refresh that ended in [LoadState.Error] is a `feed_error`, and one that
 * completed with `endOfPaginationReached` and no items is a `feed_empty`. That is the same
 * "has this actually finished?" question the empty states themselves ask, kept in one place so
 * the two screens cannot drift apart on the answer.
 *
 * A failed retry reports again on purpose — one failure is a flaky connection, a second is the
 * source being down.
 */
@Composable
fun TrackEmptyFeed(
    pagingItems: LazyPagingItems<*>,
    screen: String,
    params: Map<String, String> = emptyMap(),
    tracker: Tracker = koinInject(),
) {
    // Read through a latch: params change as the user re-anchors the feed, and restarting the
    // effect on every one of those would re-report the state the feed is already in.
    val currentParams = rememberUpdatedState(params)
    LaunchedEffect(pagingItems, screen, tracker) {
        // snapshotFlow conflates equal values, so a state the feed is already sitting in is
        // reported once, not on every recomposition.
        snapshotFlow { pagingItems.loadState.refresh to pagingItems.itemCount }
            .collect { (refresh, itemCount) ->
                if (itemCount > 0) return@collect
                when {
                    refresh is LoadState.Error ->
                        tracker.trackFeedError(screen, refresh.error, currentParams.value)

                    refresh is LoadState.NotLoading && refresh.endOfPaginationReached ->
                        tracker.trackFeedEmpty(screen, currentParams.value)
                }
            }
    }
}

/**
 * Which backend served this rover, as an analytics parameter — the page-keyed images.nasa.gov
 * library (Spirit/Opportunity) or the sol-keyed raw archive. A failure rate that splits along
 * this line points at a source rather than at the app.
 */
fun feedModeParam(roverId: Long): String = if (roverId.usesPageFeed()) "page" else "sol"
