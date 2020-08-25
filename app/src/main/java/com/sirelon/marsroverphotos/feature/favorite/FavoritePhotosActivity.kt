package com.sirelon.marsroverphotos.feature.favorite

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.util.Pair
import androidx.lifecycle.observe
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.android.synthetic.main.activity_popular_photos.*
import kotlinx.android.synthetic.main.view_native_adview.*

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

        val adapter = ViewTypeAdapter()

        viewModel.favoriteImages.observe(this, adapter::replaceData)

        adapter.addDelegateAdapter(
            AdapterConstants.MARS_PHOTO,
            FavoriteDelegateAdapter(object : OnModelChooseListener<MarsImage> {
                override fun onModelChoose(
                    model: MarsImage,
                    vararg sharedElements: Pair<View, String>
                ) {
                    val photos = adapter.getData().filterIsInstance<MarsPhoto>()

                    val ids = photos.map { it.id.toInt() }

                    val intent = ImageActivity.createIntent(this@FavoritePhotosActivity, ids, false)
                    startActivity(intent)
                }
            })
        )
        popularPhotosList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        popularPhotosList.setHasFixedSize(true)
        popularPhotosList.adapter = adapter
    }
}
