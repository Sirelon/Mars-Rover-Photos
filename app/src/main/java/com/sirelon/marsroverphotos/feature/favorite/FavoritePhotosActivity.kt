package com.sirelon.marsroverphotos.feature.favorite

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.PagedViewTypeAdapter3
import com.sirelon.marsroverphotos.adapter.diffutils.ItemDiffCallback
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.android.synthetic.main.activity_popular_photos.*
import kotlinx.android.synthetic.main.view_native_adview.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FavoritePhotosActivity : RxActivity() {

    private val viewModel by viewModels<FavoriteImagesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_photos)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        title = getString(R.string.favorite_title)
        AdvertisingObjectFactory.getAdvertisingDelegate().loadAd(adViewBanner)

        val adapter = PagedViewTypeAdapter3(ItemDiffCallback<MarsImage>())

        lifecycleScope.launch {
            viewModel.favoriteImagesFlow.collectLatest(adapter::setPagedList)
        }

        adapter.addDelegateAdapter(
            AdapterConstants.MARS_PHOTO,
            FavoriteDelegateAdapter(callback = object : OnModelChooseListener<MarsImage> {
                override fun onModelChoose(
                    model: MarsImage,
                    vararg sharedElements: Pair<View, String>
                ) {
                    val photos = adapter.getData().filterIsInstance<MarsPhoto>()

                    val ids = photos.map { it.id.toInt() }

                    val intent = ImageActivity.createIntent(this@FavoritePhotosActivity, model.id, ids, false)
                    startActivity(intent)
                }
            },
                                    favoriteCallback = {
                                        viewModel.updateFavForImage(it)
                                    })
        )
        popularPhotosList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        popularPhotosList.setHasFixedSize(true)
        popularPhotosList.adapter = adapter
    }
}
