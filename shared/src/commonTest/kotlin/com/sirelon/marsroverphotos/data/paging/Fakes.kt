package com.sirelon.marsroverphotos.data.paging

import androidx.paging.PagingSource
import com.sirelon.marsroverphotos.data.database.dao.ImagesDao
import com.sirelon.marsroverphotos.data.database.entities.MarsImage
import com.sirelon.marsroverphotos.data.database.entities.PopularUpdate
import com.sirelon.marsroverphotos.domain.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.domain.repositories.PhotosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Fake [PhotosRepository] backed by a sol -> photos map. Returns the photos configured for the
 * requested sol (empty otherwise) and records every probed sol so tests can assert scan bounds.
 */
class FakePhotosRepository(
    private val solToPhotos: Map<Long, List<MarsImage>>,
) : PhotosRepository {
    val probedSols = mutableListOf<Long>()

    override suspend fun refreshImages(query: PhotosQueryRequest): List<MarsImage> {
        probedSols += query.sol
        return solToPhotos[query.sol].orEmpty()
    }
}

/**
 * Fake [ImagesDao]. Only [insertImages], [loadImagesByIds] and [deleteNonUserImagesBeyondCount]
 * are exercised by [SolPagingSource]; the rest are minimal stubs.
 *
 * [persistedById] lets a test configure the rows the DB would return on read-back, so the
 * write-through merge in `cacheAndMerge` can be verified.
 */
class FakeImagesDao(
    private val persistedById: Map<String, MarsImage> = emptyMap(),
) : ImagesDao {
    val insertedIds = mutableListOf<String>()
    var evictionCalls = 0

    override suspend fun insertImages(images: List<MarsImage>): List<Long> {
        insertedIds += images.map { it.id }
        return emptyList()
    }

    override suspend fun loadImagesByIds(ids: List<String>): List<MarsImage> =
        ids.mapNotNull { persistedById[it] }

    override suspend fun deleteNonUserImagesBeyondCount(keepCount: Int) {
        evictionCalls++
    }

    // --- minimal stubs below (never invoked by SolPagingSource) ---
    override fun getImagesByIds(ids: List<String>): Flow<List<MarsImage>> = emptyFlow()
    override fun getAllImages(): Flow<List<MarsImage>> = emptyFlow()
    override suspend fun update(item: MarsImage) = Unit
    override suspend fun updateFavorite(id: String, favorite: Boolean, counter: Long) = Unit
    override suspend fun updatePopular(update: PopularUpdate) = Unit
    override fun loadFavoriteImages(): Flow<List<MarsImage>> = emptyFlow()
    override fun loadPopularImages(): Flow<List<MarsImage>> = emptyFlow()
    override fun loadFavoritePagedSource(): PagingSource<Int, MarsImage> = TODO("unused")
    override fun loadPopularPagedSource(): PagingSource<Int, MarsImage> = TODO("unused")
    override suspend fun clearPopularFlags() = Unit
}

/** Builds a [MarsImage] for a sol with the given id and optional camera. */
fun marsImage(
    id: String,
    sol: Long,
    cameraName: String? = null,
    cameraFullName: String = cameraName.orEmpty(),
    favorite: Long = 0,
): MarsImage = MarsImage(
    id = id,
    order = 0,
    sol = sol,
    name = "img-$id",
    imageUrl = "https://example.test/$id.jpg",
    earthDate = "2021-02-18",
    camera = cameraName?.let {
        com.sirelon.marsroverphotos.domain.models.RoverCamera(
            id = 0,
            name = it,
            fullName = cameraFullName,
        )
    },
    stats = MarsImage.Stats(see = 0, scale = 0, save = 0, share = 0, favorite = favorite),
)
