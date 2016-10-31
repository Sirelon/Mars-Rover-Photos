package com.sirelon.marsroverphotos

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import com.sirelon.marsroverphotos.adapter.MarsPhotosAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photosList.setHasFixedSize(true)

        photosList.layoutManager = GridLayoutManager(this, 2)

        val adapter = MarsPhotosAdapter()

        photosList.adapter = adapter

        val subscription = dataManager.getMarsPhotos()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ adapter.addData(it) }, Throwable::printStackTrace)
    }

    private val dataManager by lazy { DataManager() }

    private val photosList by lazy { photos_list }
}
