package com.sirelon.marsroverphotos.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.database.entities.PopularUpdate
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
import com.sirelon.marsroverphotos.platform.IFirebasePhotos.PopularCursor
import com.sirelon.marsroverphotos.utils.Logger

/**
 * Remote mediator for loading popular photos from Firebase into the local database.
 * Coordinates fetching data from Firebase and caching it locally for offline access.
 *
 * Created on 2/5/18 00:48 for Mars-Rover-Photos.
 */
@ExperimentalPagingApi
class PopularRemoteMediator(
    private val firebasePhotos: IFirebasePhotos,
    private val dao: ImagesDao
) : RemoteMediator<Int, MarsImage>() {

    private var appendCursor: PopularCursor? = null

    override suspend fun initialize(): InitializeAction {
        // A fresh mediator is created per Pager, so always refresh cached data from
        // network; returning LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
        // APPEND and PREPEND from running until REFRESH succeeds.
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MarsImage>
    ): MediatorResult {
        return try {
            loadSync(loadType, state)
        } catch (e: Exception) {
            Logger.e("PopularRemoteMediator", e) { "Error loading popular photos" }
            MediatorResult.Error(e)
        }
    }

    private suspend fun loadSync(
        loadType: LoadType,
        state: PagingState<Int, MarsImage>
    ): MediatorResult {
        if (loadType == LoadType.PREPEND) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        val refresh = loadType == LoadType.REFRESH
        if (refresh) appendCursor = null

        val cursor = if (refresh) null else appendCursor
        val orderOffset = if (refresh) 1 else state.pages.sumOf { it.data.size } + 1
        val loadSize = state.config.pageSize

        val list = firebasePhotos.loadPopularPhotos(loadSize, cursor)
            .mapIndexed { index, firebasePhoto ->
                firebasePhoto.toMarsImage(index + orderOffset)
            }

        if (list.isNotEmpty()) {
            val last = list.last()
            appendCursor = PopularCursor(
                shareCounter = last.stats.share,
                saveCounter = last.stats.save,
                scaleCounter = last.stats.scale,
                seeCounter = last.stats.see,
            )
        }

        if (refresh) {
            dao.clearPopularFlags()
        }

        val rowIds = dao.insertImages(list)
        val indexedIds = rowIds.mapIndexed { index, rowId -> rowId to index }
        val grouped = indexedIds.groupBy { it.first == -1L }

        val toUpdate = grouped[true]?.map { list[it.second] }
        toUpdate?.forEach { photo ->
            dao.updatePopular(PopularUpdate(photo.id, photo.popular, photo.order, photo.stats))
        }

        val endOfPaginationReached = list.isEmpty()
        return MediatorResult.Success(endOfPaginationReached)
    }
}
