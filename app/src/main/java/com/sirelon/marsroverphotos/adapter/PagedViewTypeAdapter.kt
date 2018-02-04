package com.sirelon.marsroverphotos.adapter

import android.arch.paging.PagedList
import android.arch.paging.PagedListAdapterHelper
import android.support.v7.recyclerview.extensions.DiffCallback
import android.support.v7.recyclerview.extensions.ListAdapterConfig
import android.support.v7.widget.RecyclerView
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 2/5/18 00:32 for Mars-Rover-Photos.
 */
class PagedViewTypeAdapter<T: ViewType>(diffCallback: DiffCallback<T>)  : ViewTypeAdapter(true) {

    private val pagedHelper = PagedListAdapterHelper(this, diffCallback);

    override fun getItemCount() = pagedHelper.itemCount

    fun setPagedList(pagedList: PagedList<T>){
        pagedHelper.setList(pagedList)
    }

    override fun getItemByPosition(position: Int) = pagedHelper.getItem(position)

//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        val item = pagedHelper.getItem(position)
//
//
////        super.onBindViewHolder(holder, position)
//    }
}
