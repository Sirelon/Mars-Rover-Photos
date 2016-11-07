package com.sirelon.marsroverphotos.adapter

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 31.10.16 on 10:57
 */
interface ViewTypeDelegateAdapter {

    fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder

    fun onBindViewHolder(holder: RecyclerView.ViewHolder, item: ViewType, payloads: MutableList<Any>? = null)
}