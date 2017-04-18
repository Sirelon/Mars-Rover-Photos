package com.sirelon.marsroverphotos.widget

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.view_image.view.*
import uk.co.senab.photoview.PhotoViewAttacher

/**
 * @author romanishin
 * @since 16.11.16 on 18:11
 */
class ViewsPagerAdapter(context: Context, val data: List<MarsPhoto>?) : PagerAdapter() {

    val picasso: Picasso

    var scaleCallback : (() -> Unit)? = null

    init {
        picasso = RoverApplication.APP.picasso()
    }

    override fun instantiateItem(container: ViewGroup?, position: Int): Any {
        container!!
        val imageRoot = container.inflate(R.layout.view_image, false)
        container.addView(imageRoot)
        showImage(imageRoot.fullscreenImage, imageRoot.fullscreenImageProgress, position)

        return imageRoot
    }

    private fun showImage(imageView: ImageView, fullscreenImageProgress: View, position: Int) {
        val photoViewAttacher = PhotoViewAttacher(imageView)

        photoViewAttacher.setOnScaleChangeListener { scaleFactor, focusX, focusY ->
            scaleCallback?.invoke()
        }

        val marsPhoto = data?.get(position) ?: return

        picasso.load(marsPhoto.imageUrl).tag(marsPhoto.id).into(imageView, object : Callback {
            override fun onSuccess() {
                fullscreenImageProgress.visibility = View.GONE
                photoViewAttacher.update()
            }

            override fun onError() {
                fullscreenImageProgress.visibility = View.GONE
                Toast.makeText(imageView.context, "Cannot show this image", Toast.LENGTH_SHORT).show()
//                Snackbar.make(imageView.rootView, "Cannot show this image", Snackbar.LENGTH_INDEFINITE)
//                        .setAction("Close", { finish() })
//                        .show()
            }
        })
    }

    override fun destroyItem(container: ViewGroup?, position: Int, viewRoot: Any?) {
        container?.removeView(viewRoot as View)
        picasso.cancelTag(data?.get(position)?.id)
    }

    override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
        return `object` == view
    }

    override fun getCount(): Int = data?.size ?: 0
}
