package com.sirelon.marsroverphotos.adapter

import android.view.ViewGroup
import androidx.collection.SparseArrayCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.adapter.diffutils.ViewTypeDiffCallack
import com.sirelon.marsroverphotos.adapter.headers.HeaderViewType
import com.sirelon.marsroverphotos.feature.rovers.RoverDiff
import com.sirelon.marsroverphotos.models.ViewType
import java.util.ArrayList

/**
 * @author romanishin
 * @since 31.10.16 on 11:32
 */
open class ViewTypeAdapter(var withLoadingView: Boolean = true) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var savedItems: ArrayList<ViewType>? = null

    private var items: ArrayList<ViewType>
    private var delegates = SparseArrayCompat<ViewTypeDelegateAdapter?>()
    protected val loadingItem = object : ViewType {
        override fun getViewId() = this
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        return delegates.get(viewType)!!.onCreateViewHolder(parent)
    }

    override fun onBindViewHolder(
        holder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        position: Int,
        payloadsArg: List<Any>
    ) {
        val item = getItemByPosition(position) ?: return
        val payloads = if (payloadsArg.isEmpty()) null else payloadsArg
        delegates.get(getItemViewType(position))?.onBindViewHolder(holder, item, payloads)
    }

    override fun onBindViewHolder(
        holder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
        position: Int
    ) {
        onBindViewHolder(holder, position, listOf())
    }

    override fun getItemViewType(position: Int): Int {
        return getItemByPosition(position)?.getViewType() ?: -1
    }

    override fun getItemCount(): Int = items.size

    open fun addData(data: List<ViewType>) {

        if (withLoadingView) {

            stopLoading()

            // insert news and the loading at the end of the list
            items.addAll(data)

            notifyDataSetChanged()
//            items.add(loadingItem)
//            notifyItemRangeChanged(initPosition, items.size + 1 /* plus loading item */)
        } else {
            val startPos = items.size
            items.addAll(data)
            notifyItemRangeInserted(startPos, items.size)
        }
    }

    open fun addOrReplace(viewType: ViewType) {
        if (items.contains(viewType)) {
            val position = items.indexOf(viewType)
            val oldModel = getItemByPosition(position) ?: return
            val changePayload = RoverDiff.getChangePayload(oldModel, viewType)
            items[position] = viewType
            notifyItemChanged(position, changePayload)
        } else {
            items.add(viewType)
            notifyItemInserted(items.size)
        }
    }

    open fun getItemByPosition(position: Int): ViewType? = items[position]

    fun stopLoading() {
        if (withLoadingView) {
            // first remove loading and notify
            val initPosition = items.size - 1

            if (initPosition >= 0) {
                items.removeAt(initPosition)
                notifyItemRemoved(initPosition)
            }
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

    open fun getData(): List<ViewType> = items

    fun getSavedData(): List<ViewType>? = savedItems

    fun applyFilteredData(filteredData: List<ViewType>) {
        if (savedItems == null) savedItems = ArrayList(items)

        replaceData(filteredData)
    }

    open fun replaceData(newData: List<ViewType>) {
        DiffUtil.calculateDiff(ViewTypeDiffCallack(items, newData)).dispatchUpdatesTo(this)

        items.clear()
        items.addAll(newData)
    }
}