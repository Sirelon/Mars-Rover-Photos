package com.sirelon.marsroverphotos.feature.popular

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import kotlinx.android.synthetic.main.activity_popular_photos.*

class PopularPhotosActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_photos)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val adapter = ViewTypeAdapter(false)

        popularPhotosList.apply {
            setHasFixedSize(true)

            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

            this.adapter = adapter
        }
    }
}
