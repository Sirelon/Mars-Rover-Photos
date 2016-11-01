package com.sirelon.marsroverphotos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.MarsPhotosDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.models.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_photo_header.*

class PhotosActivity : RxActivity(), OnModelChooseListener {

    companion object {
        val EXTRA_ROVER = ".extraRover"

        fun createIntent(context: Context, rover: Rover): Intent {
            val intent = Intent(context, PhotosActivity::class.java)
            intent.putExtra(EXTRA_ROVER, rover)
            return intent
        }
    }

    override fun onModelChoose(model: ViewType) {
        if (model is MarsPhoto) {
            startActivity(ImageActivity.createIntent(this, model))
        }
    }

    private lateinit var queryRequest: PhotosQueryRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initHeaderView()

        photosList.setHasFixedSize(true)

        photosList.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        val adapter = ViewTypeAdapter()
        adapter.addDelegateAdapter(AdapterConstants.MARS_PHOTO, MarsPhotosDelegateAdapter(this))

        photosList.adapter = adapter

        val rover = intent.getParcelableExtra<Rover>(EXTRA_ROVER)
        queryRequest = PhotosQueryRequest(rover.name, 1, null)

        loadData()
    }

    private fun loadData() {
        val subscription = dataManager.getMarsPhotos(queryRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    (photosList.adapter as ViewTypeAdapter).addData(it)
                }, Throwable::printStackTrace)


        subscriptions.add(subscription)
    }

    private fun initHeaderView() {
        dateSolChoose.setOnClickListener {
            subscriptions.clear()
            queryRequest.sol++
            loadData()
        }
    }

    private val photosList by lazy { photos_list }
}
