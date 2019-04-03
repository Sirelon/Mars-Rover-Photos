package com.sirelon.marsroverphotos.adapter.diffutils

import androidx.recyclerview.widget.DiffUtil
import com.sirelon.marsroverphotos.feature.rovers.RoverDiff
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 2/6/18 22:41 for Mars-Rover-Photos.
 */
open class ItemDiffCallback<T : ViewType> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) =
        oldItem.getViewId() == newItem.getViewId()

    override fun areContentsTheSame(oldItem: T, newItem: T) =
        oldItem == newItem

    override fun getChangePayload(oldItem: T, newItem: T): Any? {
        return RoverDiff.getChangePayload(oldItem, newItem) ?: super.getChangePayload(
            oldItem,
            newItem
        )
    }
}