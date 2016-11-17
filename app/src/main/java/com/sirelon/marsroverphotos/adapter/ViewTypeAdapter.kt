package com.sirelon.marsroverphotos.adapter

import android.support.v4.util.SparseArrayCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.adapter.diffutils.ViewTypeDiffCallack
import com.sirelon.marsroverphotos.adapter.diffutils.getChangePayload
import com.sirelon.marsroverphotos.adapter.headers.HeaderViewType
import com.sirelon.marsroverphotos.models.ViewType
import java.util.*

/**
 * @author romanishin
 * @since 31.10.16 on 11:32
 */
class ViewTypeAdapter(var withLoadingView: Boolean = true) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var savedItems: ArrayList<ViewType>? = null

    private var items: ArrayList<ViewType>
    private var delegates = SparseArrayCompat<ViewTypeDelegateAdapter?>()
    private val loadingItem = object : ViewType {
        override fun getViewType(): Int = AdapterConstants.LOADING
    }

    init {
        items = ArrayList()
        if (withLoadingView) {
            items.add(loadingItem)
            delegates.put(AdapterConstants.LOADING, LoadingDelegateAdapter())
        }
    }

    fun addDelegateAdapter(id: Int, delegateAdapter: ViewTypeDelegateAdapter) {
        delegates.put(id, delegateAdapter)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        return delegates.get(viewType)?.onCreateViewHolder(parent)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>?) {
        delegates.get(getItemViewType(position))?.onBindViewHolder(holder, items[position], payloads)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindViewHolder(holder, position, null)
    }

    override fun getItemViewType(position: Int): Int {
        return this.items[position].getViewType()
    }

    override fun getItemCount(): Int = items.size

    fun addData(data: List<ViewType>) {

        if (withLoadingView) {

            stopLoading()

            // insert news and the loading at the end of the list
            items.addAll(data)

//            items.add(loadingItem)
//            notifyItemRangeChanged(initPosition, items.size + 1 /* plus loading item */)
        } else {
            val startPos = items.size
            items.addAll(data)
            notifyItemRangeInserted(startPos, items.size)
        }
    }

    fun addOrReplace(viewType: ViewType) {
        if (items.contains(viewType)) {
            val position = items.indexOf(viewType)
            val oldModel = items[position]
            val changePayload = getChangePayload(oldModel, viewType)
            items[position] = viewType
            notifyItemChanged(position, changePayload)
        } else {
            items.add(viewType)
            notifyItemInserted(items.size)
        }
    }

    fun stopLoading(){
        if (withLoadingView){
            // first remove loading and notify
            val initPosition = items.size - 1
            items.removeAt(initPosition)
            notifyItemRemoved(initPosition)
        }
    }

    fun addHeader(headerViewType: HeaderViewType) {
        if (withLoadingView) {

            stopLoading()
            withLoadingView = false
        }

        items.add(headerViewType)
        notifyItemInserted(items.size)
    }

    fun clearAll() {
        val initPosition = items.size
        items.clear()
        notifyItemRangeRemoved(0, initPosition)

        if (withLoadingView) {
            items.add(loadingItem)
            notifyItemInserted(0)
        }
    }

    fun getData(): ArrayList<ViewType> = items

    fun getSavedData(): ArrayList<ViewType>? = savedItems

    fun applyFilteredData(filteredData: List<ViewType>) {
        if (savedItems == null)
            savedItems = ArrayList(items)

        replaceData(filteredData)
    }

    fun replaceData(newData: List<ViewType>) {
        DiffUtil.calculateDiff(ViewTypeDiffCallack(items, newData)).dispatchUpdatesTo(this)

        items.clear()
        items.addAll(newData)
    }
}