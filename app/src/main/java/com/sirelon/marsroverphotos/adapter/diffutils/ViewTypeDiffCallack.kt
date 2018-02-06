package com.sirelon.marsroverphotos.adapter.diffutils

import android.support.v7.util.DiffUtil
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 07.11.16 on 16:53
 */
class ViewTypeDiffCallack(
    private val oldData: List<ViewType>,
    private val newData: List<ViewType>,
    private val viewTypeDiffResolver: ViewTypeDiffResolver<ViewType> = ViewTypeDiffResolver()
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldData.size

    override fun getNewListSize(): Int = newData.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            viewTypeDiffResolver.areItemsTheSame(oldData[oldItemPosition], newData[newItemPosition])

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            viewTypeDiffResolver.areContentsTheSame(
                oldData[oldItemPosition], newData[newItemPosition]
            )

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        return viewTypeDiffResolver.getChangePayload(
            oldData[oldItemPosition], newData[newItemPosition]
        )
    }
}