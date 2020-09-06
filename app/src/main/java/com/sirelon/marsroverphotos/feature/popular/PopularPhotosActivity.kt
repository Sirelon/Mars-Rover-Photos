package com.sirelon.marsroverphotos.feature.popular

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.favorite.BasePhotosActivity
import com.sirelon.marsroverphotos.storage.MarsImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PopularPhotosActivity : BasePhotosActivity() {

    private val viewModel by viewModels<PopularPhotosViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.popularPhotos.collectLatest(adapter::submitData)
        }
    }

    override fun updateFavorite(image: MarsImage) {
        viewModel.updateFavorite(image)
    }

    override fun title(): CharSequence = getString(R.string.popular_title)
}