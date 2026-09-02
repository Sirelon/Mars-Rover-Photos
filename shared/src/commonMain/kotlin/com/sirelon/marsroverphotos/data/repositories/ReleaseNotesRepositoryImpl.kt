package com.sirelon.marsroverphotos.data.repositories

import com.sirelon.marsroverphotos.data.network.models.ReleaseDto
import com.sirelon.marsroverphotos.data.network.models.toDomain
import com.sirelon.marsroverphotos.domain.releasenotes.Release
import com.sirelon.marsroverphotos.domain.repositories.ReleaseNotesRepository
import com.sirelon.marsroverphotos.utils.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reads release notes from the `release-notes` Firestore collection.
 *
 * The whole collection is fetched and then filtered and sorted in Kotlin. Filtering `active`
 * server-side while ordering by `date` would need a composite index for a collection holding one
 * document per release — `FirebasePhotosImpl.loadEducationalFacts` takes the same fetch-all
 * approach for the same reason.
 *
 * Results are cached for the process: Nav3 gives each nav entry its own `ViewModelStore`, so the
 * What's New dialog, story pager and version list each construct their own `WhatsNewViewModel`.
 * Without the cache and the [Mutex] serialising first access, each would issue its own query.
 */
class ReleaseNotesRepositoryImpl : ReleaseNotesRepository {

    private companion object {
        const val TAG = "ReleaseNotesRepository"
        const val COLLECTION = "release-notes"
    }

    private val firestore get() = Firebase.firestore
    private val mutex = Mutex()
    private var cached: ImmutableList<Release>? = null

    override suspend fun getReleases(): ImmutableList<Release> = mutex.withLock {
        cached ?: fetch().also { releases ->
            // An empty result is not cached: it means the fetch failed or the collection is not
            // published yet, and the next launch should retry rather than stay empty for the process.
            if (releases.isNotEmpty()) cached = releases
        }
    }

    private suspend fun fetch(): ImmutableList<Release> = try {
        val releases = firestore.collection(COLLECTION).get().documents
            // Per document: one unparseable document must not take the other releases down with it.
            .mapNotNull { document ->
                runCatching { document.data<ReleaseDto>() }
                    .onFailure { Logger.w(TAG) { "Skipping malformed release note ${document.id}" } }
                    .getOrNull()
            }
            // `active` gates both a malformed/retracted document and a version pushed ahead of
            // store approval — either way there is nothing to show or update to yet, so it must
            // stay invisible until flipped to true. See `store-release/SKILL.md`.
            .filter { it.active }
            .mapNotNull { it.toDomain() }
            .sortedByDescending { it.date }
            .toImmutableList()
        Logger.d(TAG) { "Loaded ${releases.size} releases from Firestore" }
        releases
    } catch (e: Exception) {
        Logger.e(TAG, e) { "Failed to load release notes" }
        persistentListOf()
    }
}
