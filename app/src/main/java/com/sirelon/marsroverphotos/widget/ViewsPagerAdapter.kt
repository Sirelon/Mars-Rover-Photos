package com.sirelon.marsroverphotos.widget

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.models.MarsPhoto
import kotlinx.android.synthetic.main.view_image.view.*

/**
 * @author romanishin
 * @since 16.11.16 on 18:11
 */
class ViewsPagerAdapter(val data: List<MarsPhoto>?) : androidx.viewpager.widget.PagerAdapter() {

    var scaleCallback: (() -> Unit)? = null

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val imageRoot = container.inflate(R.layout.view_image, false)
        container.addView(imageRoot)
        showImage(imageRoot.fullscreenImage, position)

        return imageRoot
    }

    override fun destroyItem(container: ViewGroup, position: Int, viewRoot: Any) {
        container.removeView(viewRoot as View)
    }

    private fun showImage(imageView: ImageView, position: Int) {
        val photoViewAttacher = PhotoViewAttacher(imageView)

        photoViewAttacher.setOnScaleChangeListener { _, _, _ ->
            scaleCallback?.invoke()
        }

        val marsPhoto = data?.get(position) ?: return
        imageView.loadImage(marsPhoto.imageUrl)
    }

    override fun isViewFromObject(view: View, `object`: Any) = `object` == view

    override fun getCount(): Int = data?.size ?: 0
}
