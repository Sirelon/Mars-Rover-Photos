package com.sirelon.marsroverphotos.feature.rovers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.fragment.app.FragmentActivity

internal data class RoversNavActions(
    val activity: FragmentActivity,
    val navState: RoversNavigationState,
    val onHideUi: (Boolean) -> Unit,
    val onClearCache: () -> Unit,
    val onRateApp: () -> Unit,
)

internal val LocalRoversNavActions = staticCompositionLocalOf<RoversNavActions> {
    error("RoversNavActions not provided")
}

internal sealed interface RoversDestination {
    sealed interface TopLevel : RoversDestination {
        val analyticsTag: String
    }

    data object Rovers : TopLevel {
        override val analyticsTag: String = "rovers"
    }

    data object Favorite : TopLevel {
        override val analyticsTag: String = "favorite"
    }

    data object Popular : TopLevel {
        override val analyticsTag: String = "popular"
    }

    data object About : TopLevel {
        override val analyticsTag: String = "about"
    }

    data object Ukraine : RoversDestination
    data class RoverDetail(val roverId: Long) : RoversDestination
    data class ImageGallery(
        val ids: List<String>,
        val selectedId: String?,
        val shouldTrack: Boolean,
    ) : RoversDestination
}

internal class RoversNavigationState(start: RoversDestination.TopLevel) {

    private val stacks: LinkedHashMap<RoversDestination.TopLevel, SnapshotStateList<RoversDestination>> =
        linkedMapOf(start to mutableStateListOf<RoversDestination>().apply { add(start) })

    var currentTopLevel by mutableStateOf(start)
        private set

    val backStack: SnapshotStateList<RoversDestination> =
        mutableStateListOf<RoversDestination>().apply {
            add(start)
        }

    private fun createStack(destination: RoversDestination.TopLevel) =
        mutableStateListOf<RoversDestination>().apply { add(destination) }

    private fun rebuildBackStack() {
        backStack.clear()
        stacks.values.forEach { stack ->
            backStack.addAll(stack)
        }
    }

    fun selectTopLevel(destination: RoversDestination.TopLevel, resetToTop: Boolean = false) {
        val stack = stacks.remove(destination) ?: createStack(destination)
        if (resetToTop) {
            stack.clear()
            stack.add(destination)
        } else if (stack.isEmpty()) {
            stack.add(destination)
        }
        stacks[destination] = stack
        currentTopLevel = destination
        rebuildBackStack()
    }

    fun push(destination: RoversDestination, singleTop: Boolean = false) {
        if (destination is RoversDestination.TopLevel) {
            selectTopLevel(destination, resetToTop = true)
            return
        }

        val stack = stacks[currentTopLevel] ?: return
        if (singleTop) {
            removeFromStacks(destination)
        } else {
            stack.remove(destination)
        }
        stack.add(destination)
        rebuildBackStack()
    }

    private fun removeFromStacks(destination: RoversDestination) {
        stacks.values.forEach { stack ->
            stack.remove(destination)
        }
    }

    fun pop(): Boolean {
        val stack = stacks[currentTopLevel] ?: return false
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
            rebuildBackStack()
            return true
        }

        if (stacks.size == 1) {
            return false
        }

        stacks.remove(currentTopLevel)
        currentTopLevel = stacks.keys.last()
        rebuildBackStack()
        return true
    }

    fun canGoBack(): Boolean = backStack.size > 1
}
