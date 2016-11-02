package com.sirelon.marsroverphotos.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.diffutils.RoverDiff
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType, payloads: MutableList<Any>?) {
        (holder as RoverViewHolder).bind(item as Rover, payloads)
        (holder as RoverViewHolder).itemView.setOnClickListener { callback.onModelChoose(item) }
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder = RoverViewHolder(parent)

    class RoverViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_rover)) {

        fun bind(item: Rover, payloads: MutableList<Any>?) = with(itemView) {
            if (payloads == null || payloads.isEmpty()) {
                bindFullInfo(item)
            } else
                for (payload in payloads) {
                    if (payload is List<*>) {
                        for (payloadFinal in payload)
                            bindByPayload(item, payloadFinal)
                    } else
                        bindByPayload(item, payload)
                }

        }

        private fun bindByPayload(item: Rover, payloadFinal: Any?) = with(itemView) {
            when (payloadFinal) {
                RoverDiff.IMAGE -> roverPhoto.loadImage(item.iamgeUrl)
                RoverDiff.TOTAL_PHOTOS -> totalPhotos.text = "Total photos: ${item.totalPhotos}"
                RoverDiff.LAST_PHOTO_DATE -> lastPhotoDate.text = "Last photo date: ${item.maxDate}"
                RoverDiff.LAUNCH_DATE -> launchDate.text = "Launch date from Earth: ${item.launchDate}"
                RoverDiff.LANDING_DATE -> roverLandingDate.text = "Landing date on Mars: ${item.landingDate}"
                else -> {
                }
            }
        }

        private fun bindFullInfo(item: Rover) = with(itemView) {
            roverPhoto.loadImage(item.iamgeUrl)
            roverName.text = item.name
            roverStatus.text = "Status: ${item.status}"
            totalPhotos.text = "Total photos: ${item.totalPhotos}"
            lastPhotoDate.text = "Last photo date: ${item.maxDate}"
            launchDate.text = "Launch date from Earth: ${item.launchDate}"
            roverLandingDate.text = "Landing date on Mars: ${item.landingDate}"
        }
    }

}