package com.sirelon.marsroverphotos.feature.rovers

import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.recyclerview.widget.RecyclerView
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.ViewTypeDelegateAdapter
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.extensions.spannable
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.item_rover.view.*

/**
 * @author romanishin
 * @since 31.10.16 on 18:48
 */
@Keep
class RoversDelegateAdapter(private val callback: OnModelChooseListener<ViewType>) :
    ViewTypeDelegateAdapter {

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: ViewType,
        payloads: List<Any>?
    ) {
        (holder as RoverViewHolder).bind(item as Rover, payloads)
        holder.itemView.setOnClickListener { callback.onModelChoose(item) }
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        RoverViewHolder(parent)

    @Keep
    class RoverViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.item_rover)) {

        fun bind(item: Rover, payloads: List<Any>?) = if (payloads == null || payloads.isEmpty()) {
            bindFullInfo(item)
        } else
            for (payload in payloads) {
                if (payload is List<*>) {
                    for (payloadFinal in payload)
                        bindByPayload(item, payloadFinal)
                } else
                    bindByPayload(item, payload)
            }

        private fun bindByPayload(item: Rover, payloadFinal: Any?) = with(itemView) {
            when (payloadFinal) {
                RoverDiff.IMAGE -> roverPhoto loadImage item.iamgeUrl
                RoverDiff.TOTAL_PHOTOS -> totalPhotos.text =
                    spannable {
                        typeface(Typeface.BOLD) {
                            +"Total photos: "
                        }
                        +"${item.totalPhotos}"

                    }.toCharSequence()

                RoverDiff.LAST_PHOTO_DATE -> lastPhotoDate.text =
                    spannable {
                        typeface(Typeface.BOLD) {
                            +"Last photo date: "
                        }
                        +item.maxDate
                    }.toCharSequence()

                RoverDiff.LAUNCH_DATE -> launchDate.text =
                    spannable {
                        typeface(Typeface.BOLD) {
                            +"Launch date from Earth: "
                        }
                        +item.launchDate
                    }.toCharSequence()

                RoverDiff.LANDING_DATE -> roverLandingDate.text =
                    spannable {
                        typeface(Typeface.BOLD) {
                            +"Landing date on Mars: "
                        }
                        +item.landingDate
                    }.toCharSequence()

                else -> {
                }
            }
        }

        private fun bindFullInfo(item: Rover) = with(itemView) {
            roverPhoto.loadImage(item.iamgeUrl)

            roverName.text = item.name

            val statusSpanColor =
                if ("active".equals(item.status, true)) Color.rgb(85, 139, 47) else Color.RED

            roverStatus.text = spannable {
                typeface(Typeface.BOLD) {
                    +"Status: "
                }
                color(statusSpanColor) {
                    +item.status
                }
            }.toCharSequence()

            totalPhotos.text =
                spannable {
                    typeface(Typeface.BOLD) {
                        +"Total photos: "
                    }
                    +"${item.totalPhotos}"
                }.toCharSequence()

            lastPhotoDate.text =
                spannable {
                    typeface(Typeface.BOLD) {
                        +"Last photo date: "
                    }
                    +item.maxDate
                }.toCharSequence()

            launchDate.text =
                spannable {
                    typeface(Typeface.BOLD) {
                        +"Launch date from Earth: "
                    }
                    +item.launchDate
                }.toCharSequence()

            roverLandingDate.text =
                spannable {
                    typeface(Typeface.BOLD) {
                        +"Landing date on Mars: "
                    }
                    +item.landingDate
                }.toCharSequence()
        }
    }

}