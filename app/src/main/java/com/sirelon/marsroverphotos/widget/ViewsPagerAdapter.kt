package com.sirelon.marsroverphotos.widget

import androidx.viewpager.widget.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
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
        showImage(imageRoot.fullscreenImage, imageRoot.fullscreenImageProgress, position)

        return imageRoot
    }

    override fun destroyItem(container: ViewGroup, position: Int, viewRoot: Any) {
        container.removeView(viewRoot as View)
        val id = data?.get(position)?.id ?: return
        Picasso.get().cancelTag(id)
    }

    private fun showImage(imageView: ImageView, fullscreenImageProgress: View, position: Int) {
        val photoViewAttacher = PhotoViewAttacher(imageView)

        photoViewAttacher.setOnScaleChangeListener { _, _, _ ->
            scaleCallback?.invoke()
        }

        val marsPhoto = data?.get(position) ?: return

        Picasso.get().load(marsPhoto.imageUrl).tag(marsPhoto.id)
            .into(imageView, object : Callback {
                override fun onSuccess() {
                    fullscreenImageProgress.visibility = View.GONE
                    photoViewAttacher.update()
                }

                override fun onError(e: Exception) {
                    fullscreenImageProgress.visibility = View.GONE
                    Toast.makeText(imageView.context, "Cannot show this image", Toast.LENGTH_SHORT)
                        .show()
//                Snackbar.make(imageView.rootView, "Cannot show this image", Snackbar.LENGTH_INDEFINITE)
//                        .setAction("Close", { finish() })
//                        .show()
                }
            })
    }

    override fun isViewFromObject(view: View, `object`: Any) = `object` == view

    override fun getCount(): Int = data?.size ?: 0
}
