package com.sirelon.marsroverphotos

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import com.sirelon.marsroverphotos.adapter.MarsPhotosAdapter
import com.sirelon.marsroverphotos.models.MarsPhoto
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photosList.setHasFixedSize(true)

        photosList.layoutManager = GridLayoutManager(this, 2)

        val adapter = MarsPhotosAdapter()
        photosList.adapter = adapter

        val photos = mutableListOf<MarsPhoto>()

        for (i in 1..25) {
            photos.add(MarsPhoto(
                    "Name $i",
                    "http://lorempixel.com/200/200/?fake=$i"
            ))
        }

        adapter.addData(photos)
    }

    private val photosList by lazy { photos_list }
}
