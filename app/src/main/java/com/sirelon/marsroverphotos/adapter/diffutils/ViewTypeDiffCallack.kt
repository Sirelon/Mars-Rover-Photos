package com.sirelon.marsroverphotos.adapter.diffutils

import android.support.v7.util.DiffUtil
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 07.11.16 on 16:53
 */
class ViewTypeDiffCallack(val oldData: List<ViewType>, val newData: List<ViewType>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldData.size

    override fun getNewListSize(): Int = newData.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newData[newItemPosition]
        val oldItem = oldData[oldItemPosition]
        val a1 = newItem.hashCode()
        val a2 = oldItem.hashCode()

        return newItem.hashCode() == oldItem.hashCode()
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newData[newItemPosition]
        val oldItem = oldData[oldItemPosition]

        return newItem == oldItem
    }
}