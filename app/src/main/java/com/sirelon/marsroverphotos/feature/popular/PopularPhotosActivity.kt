package com.sirelon.marsroverphotos.feature.popular

import android.arch.paging.PagedListAdapterHelper
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.GuardedBy
import android.support.v7.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.MarsPhotosDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.feature.firebase.toMarsPhoto
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_popular_photos.*

class PopularPhotosActivity : RxActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_photos)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val adapter = ViewTypeAdapter(true)

        adapter.addDelegateAdapter(
            AdapterConstants.POPULAR_PHOTO,
            PopularPhotosDelegateAdapter(callback = object :
                OnModelChooseListener {
                override fun onModelChoose(model: ViewType) {
                    model as FirebasePhoto
                    model.logD()
                }
            })
        )
        popularPhotosList.apply {
            setHasFixedSize(true)

            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

            this.adapter = adapter
        }

        val firebasePhotos = FirebaseProvider.firebasePhotos

        val subscribe = firebasePhotos.loadPopularPhotos()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                adapter.addData(it)
            }, Throwable::printStackTrace)

        subscriptions.add(subscribe)
    }
}
