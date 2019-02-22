package com.sirelon.marsroverphotos.adapter

import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import com.sirelon.marsroverphotos.models.ViewType

/**
 * @author romanishin
 * @since 31.10.16 on 10:57
 */
interface ViewTypeDelegateAdapter {

    fun onCreateViewHolder(parent: ViewGroup): androidx.recyclerview.widget.RecyclerView.ViewHolder

    fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, item: ViewType, payloads: List<Any>? = null)
}