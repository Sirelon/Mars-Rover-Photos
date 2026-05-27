package com.sirelon.marsroverphotos.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.get
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Renders a navigation entry whose composable already owns a Dialog/AlertDialog as an overlay.
 *
 * Navigation 3's built-in DialogSceneStrategy wraps entry content in another Dialog. The Photos
 * picker entries already create their own CMP dialogs, so this strategy only keeps the previous
 * scene underneath and lets the entry content render its existing dialog window.
 */
internal class DialogOverlaySceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val lastEntry = entries.lastOrNull() ?: return null
        val isDialogOverlay = lastEntry.metadata[DialogOverlayKey] ?: return null
        if (!isDialogOverlay || entries.size < MIN_OVERLAY_ENTRY_COUNT) return null

        return DialogOverlayScene(
            key = lastEntry.contentKey,
            entry = lastEntry,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
        )
    }

    internal companion object {
        private const val MIN_OVERLAY_ENTRY_COUNT = 2

        private object DialogOverlayKey : NavMetadataKey<Boolean>

        fun dialogOverlay(): Map<String, Any> = metadata {
            put(DialogOverlayKey, true)
        }
    }
}

private class DialogOverlayScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        entry.Content()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DialogOverlayScene<*>

        return key == other.key &&
            entry == other.entry &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + entry.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        return result
    }
}
