package com.sirelon.marsroverphotos.adapter

import android.arch.paging.AsyncPagedListDiffer
import android.arch.paging.PagedList
import android.support.v7.recyclerview.extensions.AsyncDifferConfig
import android.support.v7.util.AdapterListUpdateCallback
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
