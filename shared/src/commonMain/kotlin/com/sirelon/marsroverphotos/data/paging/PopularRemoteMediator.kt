package com.sirelon.marsroverphotos.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.database.entities.StatsUpdate
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

    private var isFirstLoad = true

    override suspend fun initialize(): InitializeAction {
        return if (isFirstLoad) {
            isFirstLoad = false
            // Need to refresh cached data from network; returning
            // LAUNCH_INITIAL_REFRESH here will also block RemoteMediator's
            // APPEND and PREPEND from running until REFRESH succeeds.
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            // Cached data is up-to-date, so there is no need to re-fetch
            // from the network.
            InitializeAction.SKIP_INITIAL_REFRESH
        }
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
        // Calculate how many items are already in the database
        val alreadyInDb = state.pages.sumOf { it.data.size } + 1

        // Don't prepend - popular photos are only appended
        if (loadType == LoadType.PREPEND) {
            return MediatorResult.Success(endOfPaginationReached = true)
        }

        try {
            val loadSize = state.config.pageSize
            val lastPhotoId = state.lastItemOrNull()?.id

            // Load popular photos from Firebase
            val list = firebasePhotos.loadPopularPhotos(loadSize, lastPhotoId)
                .mapIndexed { index, firebasePhoto ->
                    firebasePhoto.toMarsImage(index + alreadyInDb)
                }

            // Insert into database with transaction
            dao.withDaoTransaction {
                val rowIds = dao.insertImages(list)
                val indexedIds = rowIds.mapIndexed { index, rowId -> rowId to index }
                val grouped = indexedIds.groupBy { it.first == -1L }

                // Update stats for photos that already exist (insert returned -1)
                val toUpdate = grouped[true]?.map { list[it.second] }
                toUpdate?.forEach { photo ->
                    dao.updateStats(StatsUpdate(photo.id, photo.stats))
                }
            }

            val endOfPaginationReached = list.isEmpty()
            return MediatorResult.Success(endOfPaginationReached)
        } catch (e: Exception) {
            Logger.e("PopularRemoteMediator", e) { "Error in loadSync" }
            return MediatorResult.Error(e)
        }
    }
}
