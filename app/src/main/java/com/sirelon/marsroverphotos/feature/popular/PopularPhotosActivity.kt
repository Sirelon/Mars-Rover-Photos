package com.sirelon.marsroverphotos.feature.popular

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.favorite.BasePhotosActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

//class PopularPhotosActivity : RxActivity() {
//
//    private lateinit var userList: LiveData<PagedList<FirebasePhoto>>
//
//    private val pagedCallback: PagedList.Callback = object : PagedList.Callback() {
//        override fun onChanged(position: Int, count: Int) {
//
//        }
//
//        override fun onInserted(position: Int, count: Int) {
//            userList.value?.removeWeakCallback(this)
//        }
//
//        override fun onRemoved(position: Int, count: Int) {
//
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_popular_photos)
//        dataManager.lastPhotosRequest = null
//
//        setSupportActionBar(toolbar)
//
//        supportActionBar?.setDisplayShowTitleEnabled(false)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setDisplayShowHomeEnabled(true)
//
//        title = getString(R.string.popular_title)
//        AdvertisingObjectFactory.getAdvertisingDelegate().loadAd(adViewBanner)
//
//        val config =
//            AsyncDifferConfig.Builder<FirebasePhoto>(ItemDiffCallback<FirebasePhoto>()).build()
//        val adapter = PagedViewTypeAdapter(config)
//
//        val pagedListConfig =
//            PagedList.Config.Builder().setEnablePlaceholders(false).setPrefetchDistance(2)
//                .setPageSize(10).build()
//
//        val dataSourceFactory = PopularDataSourceFactory(FirebaseProvider.firebasePhotos)
//
//        userList = LivePagedListBuilder(dataSourceFactory, pagedListConfig).build()
//
//        userList.observe(this, Observer {
//            it?.addWeakCallback(null, pagedCallback)
//            adapter.setPagedList(it)
//        })
//
//        adapter.addDelegateAdapter(
//            AdapterConstants.POPULAR_PHOTO,
//            PopularPhotosDelegateAdapter(object : OnModelChooseListener<FirebasePhoto> {
//                override fun onModelChoose(
//                    model: FirebasePhoto,
//                    vararg sharedElements: Pair<View, String>
//                ) {
//                    openPhoto(model)
//                }
//            })
//        )
//        popularPhotosList.layoutManager =
//            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
//        popularPhotosList.setHasFixedSize(true)
//        popularPhotosList.adapter = adapter
//
//        val firebasePhotos = FirebaseProvider.firebasePhotos
//
//        val subscribe = firebasePhotos.loadPopularPhotos().subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(adapter::addData, Throwable::printStackTrace)
//
//        subscriptions.add(subscribe)
//    }
//
//    private fun openPhoto(photo: FirebasePhoto) {
//        // TODO: Implement it
////        val marsPhoto = photo.toMarsPhoto()
////        startActivity(ImageActivity.createIntent(this, marsPhoto, false))
//    }
//}

class PopularPhotosActivity : BasePhotosActivity() {

    private val viewModel by viewModels<PopularPhotosViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.popularPhotos.collectLatest(adapter::submitData)
        }
    }

}