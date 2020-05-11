package com.sirelon.marsroverphotos.feature.rovers

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.core.util.Pair
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.PhotosActivity
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.popular.PopularDelegateAdapter
import com.sirelon.marsroverphotos.feature.popular.PopularItem
import com.sirelon.marsroverphotos.feature.popular.PopularPhotosActivity
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import kotlinx.android.synthetic.main.activity_rovers.*

class RoversActivity : RxActivity(), OnModelChooseListener<ViewType> {

    override fun onModelChoose(model: ViewType, vararg sharedElements: Pair<View, String>) {
        when (model) {
            is Rover -> startActivity(PhotosActivity.createIntent(this, model))
            is PopularItem -> startActivity(Intent(this, PopularPhotosActivity::class.java))
//            is PopularItem -> FirebaseProvider.proideTestFirebase.deleteUnusedItems()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rovers)

        val adapter = ViewTypeAdapter(false)
        adapter.addDelegateAdapter(
            AdapterConstants.ROVER,
            RoversDelegateAdapter(this@RoversActivity)
        )
        adapter.addDelegateAdapter(
            AdapterConstants.POPULAR_ITEM,
            PopularDelegateAdapter(this@RoversActivity)
        )

        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val spanCount = if (portrait) 1 else 2

        roversList.apply {
            setHasFixedSize(true)

            layoutManager = GridLayoutManager(this@RoversActivity, spanCount).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when {
                            portrait -> 1
                            position == 0 -> 2
                            else -> 1
                        }
                    }
                }
            }

            this.adapter = adapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        val popularItem = PopularItem()
        dataManager.rovers.observe(this) { list ->
            val mutableList = list.toMutableList<ViewType>()
            mutableList.logD()
            mutableList.add(0, popularItem)
            adapter.replaceData(mutableList)
        }
    }

}
