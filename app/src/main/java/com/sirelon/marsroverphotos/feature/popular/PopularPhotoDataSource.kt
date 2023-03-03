package com.sirelon.marsroverphotos.feature.popular

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.sirelon.marsroverphotos.firebase.photos.IFirebasePhotos
import com.sirelon.marsroverphotos.storage.ImagesDao
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.storage.StatsUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * Created on 2/5/18 00:48 for Mars-Rover-Photos.
 */
// A bit hacky, I just dont want to store this field at my db.
private var firstCreation = true

@ExperimentalPagingApi
class PopularRemoteMediator(
    private val firebasePhotos: IFirebasePhotos,
    private val dao: ImagesDao
) : RemoteMediator<Int, MarsImage>() {

    override suspend fun initialize(): InitializeAction {
        return if (firstCreation) {
            firstCreation = false
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
    ) = withContext(Dispatchers.IO) {
        loadSync(loadType, state)
    }

    private suspend fun loadSync(
        loadType: LoadType,
        state: PagingState<Int, MarsImage>
    ): MediatorResult {
        val alreadyInDb = state.pages.sumOf { it.data.size } + 1
        if (loadType == LoadType.PREPEND) return MediatorResult.Success(endOfPaginationReached = true)

        try {
            val loadSize = state.config.pageSize
            val list =
                firebasePhotos.loadPopularPhotos(loadSize, state.lastItemOrNull()?.id)
                    .mapIndexed { index, item -> item.toMarsImage(index + alreadyInDb) }

            dao.withTransaction {
                val ids = dao.insertImages(list).mapIndexed { index, rowId -> rowId to index }
                val grouped = ids.groupBy { it.first == -1L }
                val toUpdate = grouped[true]?.map { list[it.second] }

                toUpdate?.forEach {
                    dao.updateStats(StatsUpdate(it.id, it.stats))
                }
            }

            val endOfPaginationReached: Boolean = list.lastOrNull() == null
            return MediatorResult.Success(endOfPaginationReached)
        } catch (e: Throwable) {
            return MediatorResult.Error(e)
        }
    }

}