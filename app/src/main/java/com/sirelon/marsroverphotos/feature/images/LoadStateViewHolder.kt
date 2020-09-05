package com.sirelon.marsroverphotos.feature.images

import android.view.View
import android.view.ViewGroup
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.logD

/**
 * Created on 31.08.2020 23:15 for Mars-Rover-Photos.
 */
class LoadStateViewHolder(val parent: ViewGroup) :
    RecyclerView.ViewHolder(parent.inflate(R.layout.load_state_item)) {

    init {
        if (itemView.layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            (itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams).isFullSpan = true
        }
    }

    private val progress: View = itemView.findViewById(R.id.loadStateProgress)

    fun bind(loadState: LoadState) {

        loadState.logD()
        progress.visibility = View.VISIBLE
    }
}

class LoadAdapter : LoadStateAdapter<LoadStateViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ) = LoadStateViewHolder(parent)

    override fun onBindViewHolder(
        holder: LoadStateViewHolder,
        loadState: LoadState
    ) = holder.bind(loadState)
}