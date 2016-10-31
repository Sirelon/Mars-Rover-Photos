package com.sirelon.marsroverphotos.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.inflate
import com.sirelon.marsroverphotos.loadImage
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.item_rover.view.*

/**
 * @author romanishin
 * @since 31.10.16 on 18:48
 */
class RoversDelegateAdapter(val callback: OnModelChooseListener) : ViewTypeDelegateAdapter {

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = RoverViewHolder(parent)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType) {
        (holder as RoverViewHolder).bind(item as Rover)
    }

    class RoverViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_rover)) {

        fun bind(item: Rover) = with(itemView) {
            roverPhoto.loadImage(item.iamgeUrl)
            roverName.text = item.name
            roverStatus.text = "Status: ${item.status}"
            totalPhotos.text = "Total photos: ${item.totalPhotos}"
            lastPhotoDate.text = "Last photo date: ${item.maxDate}"
        }
    }

}