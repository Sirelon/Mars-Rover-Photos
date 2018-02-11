package com.sirelon.marsroverphotos.feature.popular

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.os.Bundle
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.PagedViewTypeAdapter
import com.sirelon.marsroverphotos.adapter.diffutils.ViewTypeDiffResolver
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.feature.firebase.FirebasePhoto
import com.sirelon.marsroverphotos.feature.firebase.toMarsPhoto
import com.sirelon.marsroverphotos.firebase.photos.FirebaseProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_popular_photos.*
import kotlinx.android.synthetic.main.view_native_adview.*

class PopularPhotosActivity : RxActivity() {

    private lateinit var userList: LiveData<PagedList<FirebasePhoto>>

    private val pagedCallback: PagedList.Callback = object : PagedList.Callback() {
        override fun onChanged(position: Int, count: Int) {

        }

        override fun onInserted(position: Int, count: Int) {
            popularProgressBar.visibility = View.GONE
            userList.value?.removeWeakCallback(this)
        }

        override fun onRemoved(position: Int, count: Int) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_photos)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        supportActionBar?.title = getString(R.string.popular_title)
        AdvertisingObjectFactory.getAdvertisingDelegate()
                .loadAd(adViewBanner)

        val diffCallback = ViewTypeDiffResolver<FirebasePhoto>()

        val adapter = PagedViewTypeAdapter(diffCallback)

        val pagedListConfig = PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setPrefetchDistance(2)
                .setPageSize(10)
                .build()

        val dataSourceFactory = PopularDataSourceFactory(FirebaseProvider.firebasePhotos)

        userList = LivePagedListBuilder(dataSourceFactory, pagedListConfig).build()

        userList.observe(this, Observer {
            it?.addWeakCallback(null, pagedCallback)
            adapter.setPagedList(it)
        })

        adapter.addDelegateAdapter(AdapterConstants.POPULAR_PHOTO,
                                   PopularPhotosDelegateAdapter(::openPhoto))
        popularPhotosList.apply {
            setHasFixedSize(true)

            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

            this.adapter = adapter
        }

        val firebasePhotos = FirebaseProvider.firebasePhotos

        val subscribe = firebasePhotos.loadPopularPhotos()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ adapter.addData(it) }, Throwable::printStackTrace)

        subscriptions.add(subscribe)
    }

    private fun openPhoto(photo: FirebasePhoto) {
        val marsPhoto = photo.toMarsPhoto()
        startActivity(ImageActivity.createIntent(this, marsPhoto, false))
    }
}
