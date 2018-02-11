package com.sirelon.marsroverphotos.feature.advertising

import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.models.ViewType

/**
 * Created on 2/11/18 12:57 for Mars-Rover-Photos.
 */
internal object adMobDelegate : AdvertisingDelegate {

    override fun integregrateAdToList(
            dataList: List<out ViewType>): MutableList<out ViewType> {

        var step = 30

        val data: MutableList<ViewType> = dataList.toMutableList()

        if (step >= data.size / 2) {
            val advertizingItem = object : ViewType {
                override fun getViewType(): Int = AdapterConstants.ADVERTIZING
            }
            data.add(data.size, advertizingItem)
            return data
        }

        if (step >= data.size) {
            step = data.size / 2 + 1
        }

        IntProgression.fromClosedRange(step, data.size, step)
                .map {
                    val advertizingItem = object : ViewType {
                        override fun getViewType(): Int = AdapterConstants.ADVERTIZING
                    }
                    data.add(it, advertizingItem)
                }
        return data
    }

}