package com.sirelon.marsroverphotos.adapter

import androidx.paging.AsyncPagedListDiffer
import androidx.paging.PagedList
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 2/5/18 00:32 for Mars-Rover-Photos.
 */
class PagedViewTypeAdapter<T : ViewType>(diffCallback: AsyncDifferConfig<T>) :
    ViewTypeAdapter(false) {

    private val pagedHelper: AsyncPagedListDiffer<T>

    init {
        val listUpdateCallback = AdapterListUpdateCallback(this)
        pagedHelper = AsyncPagedListDiffer(listUpdateCallback, diffCallback)
    }

    override fun getItemCount() = pagedHelper.itemCount

    fun setPagedList(pagedList: PagedList<T>?) = pagedHelper.submitList(pagedList)

    override fun getItemByPosition(position: Int) = pagedHelper.getItem(position)
}
