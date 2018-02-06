package com.sirelon.marsroverphotos.feature.popular

import android.arch.lifecycle.Observer
import android.arch.paging.DataSource
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
import android.arch.paging.PagedList
import android.arch.paging.LivePagedListBuilder
import android.support.v4.view.PagerAdapter
import android.support.v7.recyclerview.extensions.DiffCallback
import com.sirelon.marsroverphotos.adapter.PagedViewTypeAdapter
import com.sirelon.marsroverphotos.adapter.diffutils.ViewTypeDiffCallack
import com.sirelon.marsroverphotos.adapter.diffutils.ViewTypeDiffResolver


class PopularPhotosActivity : RxActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_photos)

        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val diffCallback = ViewTypeDiffResolver<FirebasePhoto>()

        val adapter = PagedViewTypeAdapter(diffCallback)

        val pagedListConfig = PagedList.Config.Builder().setEnablePlaceholders(true)
            .setPrefetchDistance(10).setPageSize(20).build()

        val dataSourceFactory = PopularDataSourceFactory()

        val userList = LivePagedListBuilder(dataSourceFactory, pagedListConfig).build()

        userList.observe(this, Observer {
            it ?: return@Observer
            adapter.setPagedList(it)
        })
        // For test
//        val pagedList = PagedList.Builder(dataSourceFactory.create(), pagedListConfig).build()
//        adapter.setPagedList(pagedList);

        adapter.addDelegateAdapter(
            AdapterConstants.POPULAR_PHOTO,
            PopularPhotosDelegateAdapter(callback = object : OnModelChooseListener {
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

        val subscribe = firebasePhotos.loadPopularPhotos().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe({
            adapter.addData(it)
        }, Throwable::printStackTrace)

        subscriptions.add(subscribe)
    }
}
