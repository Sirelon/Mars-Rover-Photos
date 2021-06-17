package com.sirelon.marsroverphotos.feature.favorite

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.activity.ImageActivity
import com.sirelon.marsroverphotos.activity.RxActivity
import com.sirelon.marsroverphotos.feature.images.ImagesAdapter
import com.sirelon.marsroverphotos.feature.images.ImagesAdapterClickListener
import com.sirelon.marsroverphotos.feature.images.LoadAdapter
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BasePhotosActivity : RxActivity(), ImagesAdapterClickListener {
    val adapter = ImagesAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_popular_photos)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.title = title()

        val popularPhotosList = findViewById<RecyclerView>(R.id.popularPhotosList)
        popularPhotosList.setItemViewCacheSize(30)
        popularPhotosList.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
//        popularPhotosList.setHasFixedSize(true)

        popularPhotosList.adapter =
            adapter.withLoadStateFooter(footer = LoadAdapter())
    }

    override fun openPhoto(image: MarsImage, vararg sharedElements: View) {
        val ids = adapter.snapshot().mapNotNull { it?.id }

        val intent = ImageActivity.createIntent(this@BasePhotosActivity, image.id, ids, false)
        startActivity(intent)
    }

    abstract fun title(): CharSequence
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

    override fun updateFavorite(image: MarsImage) {
        viewModel.updateFavForImage(image)
    }

    override fun title(): CharSequence = getString(R.string.favorite_title)

}