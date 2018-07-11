package com.sirelon.marsroverphotos.adapter.diffutils

import android.support.v7.util.DiffUtil

/**
 * Created on 2/6/18 22:41 for Mars-Rover-Photos.
 */
open class ItemDiffCallback<T> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: T, newItem: T) =
        oldItem?.hashCode() == newItem?.hashCode()
}