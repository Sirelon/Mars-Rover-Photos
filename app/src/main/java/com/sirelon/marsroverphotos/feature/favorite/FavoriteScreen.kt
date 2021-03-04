package com.sirelon.marsroverphotos.feature.favorite

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.feature.MarsImageComposable
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * Created on 01.03.2021 22:32 for Mars-Rover-Photos.
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoritePhotosContent(items: LazyPagingItems<MarsImage>) {
    // If I dont call it, paging doesn't work.
//    items.loadState
    val scrollState = rememberLazyListState()
    LazyColumn(state = scrollState, contentPadding = PaddingValues(16.dp), content = {
        if (items.loadState.refresh == LoadState.Loading) {
            item {
                Column(
                    modifier = Modifier.fillParentMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        items(items.itemCount) {
            val image = items[it]
            if (image != null) {

                MarsImageComposable(marsImage = image)
            } else
                Image(
                    painter = painterResource(id = R.drawable.img_placeholder),
                    contentDescription = ""
                )
        }

        if (items.loadState.append == LoadState.Loading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    })


//    LazyVerticalGrid(state = scrollState, cells = GridCells.Fixed(2), content = {
//        if (items.loadState.refresh == LoadState.Loading) {
//            item {
//                Column(
//                    modifier = Modifier.fillParentMaxSize(),
//                    verticalArrangement = Arrangement.Center,
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    CircularProgressIndicator()
//                }
//            }
//        }
//
//        items(items.itemCount) {
//            val image = items[it]
//            if (image != null) {
//
//                ImageItem(marsImage = image)
//            } else
//                Image(
//                    painter = painterResource(id = R.drawable.img_placeholder),
//                    contentDescription = ""
//                )
//        }
//
//        if (items.loadState.append == LoadState.Loading) {
//            item {
//                CircularProgressIndicator(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .wrapContentWidth(Alignment.CenterHorizontally)
//                )
//            }
//        }
//    })


//    val nColumns = 2
//    val rows = (items.itemCount + nColumns - 1) / nColumns
//    LazyColumn(
////        modifier = modifier,
////        state = state,
////        contentPadding = contentPadding
//    ) {
//        items(rows) { rowIndex ->
//            Row {
//                for (columnIndex in 0 until nColumns) {
//                    val itemIndex = rowIndex * nColumns + columnIndex
//                    if (itemIndex < items.itemCount) {
//                        Box(
//                            modifier = Modifier.weight(1f, fill = true),
//                            propagateMinConstraints = true
//                        ) {
////                            scope.contentFor(itemIndex, this@items).invoke()
//                            val image = items[itemIndex]
//                            if (image != null)
//                                ImageItem(marsImage = image)
//                        }
//                    } else {
//                        Spacer(Modifier.weight(1f, fill = true))
//                    }
//                }
//            }
//        }
//    }


//val state = rememberScrollState()
//    StaggeredVerticalGrid(modifier = Modifier.verticalScroll(state = state), maxColumnWidth = 200.dp) {
//        (0 until items.itemCount).onEach {
//            val marsImage = items[it]
//            if (marsImage != null)
//                ImageItem(marsImage = marsImage)
//        }
//    }

//    LazyColumn {
//        itemsIndexed(items) { index, image ->
//            val first = image
//            val second = kotlin.runCatching { items.get(index + 1) }.getOrNull()
//
//            listOf<Int>().chunked(2)
//
//            Row {
//                if (first != null) {
//                    Box(
//                        modifier = Modifier
//                            .weight(1F)
//                            .align(Alignment.Top)
//                            .padding(8.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        ImageItem(marsImage = first)
//                    }
//                }
//                if (second != null) {
//                    Box(
//                        modifier = Modifier
//                            .weight(1F)
//                            .align(Alignment.Top)
//                            .padding(8.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        ImageItem(marsImage = second)
//                    }
//                }
//            }
//        }
////        items(items) {
////            ImageItem(it!!)
////        }
//    }
}
