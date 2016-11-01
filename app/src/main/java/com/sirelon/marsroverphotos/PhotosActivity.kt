package com.sirelon.marsroverphotos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.MarsPhotosDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.ViewType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*

class PhotosActivity : RxActivity(), OnModelChooseListener {

    companion object {
        val EXTRA_QUERY_REQUEST = ".extraQueryRequest"
        fun createIntent(context: Context, queryRequest: PhotosQueryRequest): Intent {
            val intent = Intent(context, PhotosActivity::class.java)
            intent.putExtra(EXTRA_QUERY_REQUEST, queryRequest)
            return intent
        }
    }

    override fun onModelChoose(model: ViewType) {
        if (model is MarsPhoto) {
            startActivity(ImageActivity.createIntent(this, model))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photosList.setHasFixedSize(true)

        photosList.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        val adapter = ViewTypeAdapter()
        adapter.addDelegateAdapter(AdapterConstants.MARS_PHOTO, MarsPhotosDelegateAdapter(this))
        photosList.adapter = adapter

        val queryRequest = intent?.getParcelableExtra<PhotosQueryRequest>(EXTRA_QUERY_REQUEST) ?: PhotosQueryRequest("curiosity", 1000, null)

        val subscription = dataManager.getMarsPhotos(queryRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ adapter.addData(it) }, Throwable::printStackTrace)


        subscriptions.add(subscription)
    }

    private val photosList by lazy { photos_list }
}
