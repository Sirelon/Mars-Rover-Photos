package com.sirelon.marsroverphotos.feature.favorite

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.feature.images.ImagesAdapter
import com.sirelon.marsroverphotos.feature.images.LoadAdapter
import kotlinx.android.synthetic.main.activity_popular_photos.*
import kotlinx.android.synthetic.main.view_native_adview.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class BasePhotosActivity : RxActivity() {
    val adapter = ImagesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_photos)
        setSupportActionBar(toolbar)
adapter.snapshot()

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        title = getString(R.string.favorite_title)
        AdvertisingObjectFactory.getAdvertisingDelegate().loadAd(adViewBanner)

//        adapter.addDelegateAdapter(
//            AdapterConstants.MARS_PHOTO,
//            FavoriteDelegateAdapter(callback = object : OnModelChooseListener<MarsImage> {
//                override fun onModelChoose(
//                    model: MarsImage,
//                    vararg sharedElements: Pair<View, String>
//                ) {
//                    val photos = adapter.getData().filterIsInstance<MarsImage>()
//
//                    val ids = photos.map { it.id }
//
//                    val intent = ImageActivity.createIntent(this@BasePhotosActivity, model.id, ids, false)
//                    startActivity(intent)
//                }
//            },
//                                    favoriteCallback = {
////                                        viewModel.updateFavForImage(it)
//                                    })
//        )
        popularPhotosList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        popularPhotosList.setHasFixedSize(true)
        lifecycleScope.launch {
            adapter.loadStateFlow.collectLatest {
                it.logD()
            }
        }
        popularPhotosList.adapter =
            adapter.withLoadStateHeaderAndFooter(header = LoadAdapter(), footer = LoadAdapter())

    }
}

/**
 * Created on 31.08.2020 22:07 for Mars-Rover-Photos.
 */
class FavoritePhotosActivity : BasePhotosActivity() {

    private val viewModel by viewModels<FavoriteImagesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            viewModel.favoriteImagesFlow.collectLatest(adapter::submitData)
        }
    }

}