package com.sirelon.marsroverphotos.adapter.diffutils

import android.support.v7.recyclerview.extensions.DiffCallback
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 2/6/18 22:41 for Mars-Rover-Photos.
 */
class ViewTypeDiffResolver<T : ViewType> : DiffCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = newItem.hashCode() == oldItem.hashCode()

    override fun areContentsTheSame(oldItem: T, newItem: T) = newItem == oldItem
}