package com.sirelon.marsroverphotos.widget

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.models.MarsPhoto
import kotlinx.android.synthetic.main.view_image.view.*

/**
 * @author romanishin
 * @since 16.11.16 on 18:11
 */
class ViewsPagerAdapter(val data: List<MarsPhoto>?) : PagerAdapter() {

    var scaleCallback: (() -> Unit)? = null

    private val colorsArr = listOf<Int>(
        Color.CYAN,
        Color.BLUE,
        Color.BLACK,
        Color.GRAY,
        Color.MAGENTA,
        Color.DKGRAY,
        Color.YELLOW,
        Color.RED,
        Color.GREEN
    )

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

        val drawable = CircularProgressDrawable(imageView.context)
        drawable.strokeWidth = 15f
        drawable.setStyle(CircularProgressDrawable.LARGE)
        drawable.setColorSchemeColors(*colorsArr.shuffled().toIntArray())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable.applyTheme(imageView.resources.newTheme())
        }
        drawable.start()

        Glide.with(imageView)
            .load(marsPhoto.imageUrl)
            .placeholder(drawable)
            .into(imageView)
    }

    override fun isViewFromObject(view: View, `object`: Any) = `object` == view

    override fun getCount(): Int = data?.size ?: 0
}
