package com.sirelon.marsroverphotos.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.database.entities.PopularUpdate
import com.sirelon.marsroverphotos.platform.IFirebasePhotos
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
        // Don't prepend - popular photos are only appended
        if (loadType == LoadType.PREPEND) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        val refresh = loadType == LoadType.REFRESH

        // REFRESH restarts ranking from the top; only APPEND continues the Firebase
        // cursor after the last loaded item and offsets `order` by what's already loaded.
        val lastPhotoId = if (refresh) null else state.lastItemOrNull()?.id
        val orderOffset = if (refresh) 1 else state.pages.sumOf { it.data.size } + 1

        val loadSize = state.config.pageSize

        // Load popular photos from Firebase
        val list = firebasePhotos.loadPopularPhotos(loadSize, lastPhotoId)
            .mapIndexed { index, firebasePhoto ->
                firebasePhoto.toMarsImage(index + orderOffset)
            }

        if (refresh) {
            // Reset stale popular flags so photos that dropped off Firebase's popular
            // list don't stay popular forever and stale rows don't collide with this
            // batch's order values 1..N. Flag-reset instead of delete: deleting would
            // drop favorited photos and rows cached by the sol feed.
            dao.clearPopularFlags()
        }

        val rowIds = dao.insertImages(list)
        val indexedIds = rowIds.mapIndexed { index, rowId -> rowId to index }
        val grouped = indexedIds.groupBy { it.first == -1L }

        // Rows that already exist (insert returned -1, e.g. cached by the sol feed with
        // popular = 0) are IGNOREd by the insert, so apply the popular flag, the fresh
        // ranking order and stats via a partial update.
        val toUpdate = grouped[true]?.map { list[it.second] }
        toUpdate?.forEach { photo ->
            dao.updatePopular(PopularUpdate(photo.id, photo.popular, photo.order, photo.stats))
        }

        val endOfPaginationReached = list.isEmpty()
        return MediatorResult.Success(endOfPaginationReached)
    }
}
