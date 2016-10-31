package com.sirelon.marsroverphotos.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewParent
import com.sirelon.marsroverphotos.ViewType

/**
 * @author romanishin
 * @since 31.10.16 on 10:57
 */
interface ViewTypeDelegateAdapter {

    fun onCreateViewHolder(parent: ViewParent) : RecyclerView.ViewHolder

    fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType)

}