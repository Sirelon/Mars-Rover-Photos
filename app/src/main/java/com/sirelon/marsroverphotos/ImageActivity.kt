package com.sirelon.marsroverphotos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_image.*
import uk.co.senab.photoview.PhotoViewAttacher

class ImageActivity : AppCompatActivity() {

    companion object {
        val EXTRA_PHOTO = ".extraPhoto"
        fun createIntent(context: Context, photo: MarsPhoto): Intent {
            val intent = Intent(context, ImageActivity::class.java)
            intent.putExtra(EXTRA_PHOTO, photo)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image)

        val marsPhoto = intent.getParcelableExtra<MarsPhoto>(EXTRA_PHOTO)

        title = "Photo id ${marsPhoto.id}. Date ${marsPhoto.earthDate}"

        val photoViewAttacher = PhotoViewAttacher(fullscreenImage)

        Picasso.with(this).load(marsPhoto.imageUrl).into(fullscreenImage, object : Callback {
            override fun onSuccess() {
                fullscreenImageProgress.visibility = View.GONE
                photoViewAttacher.update()
            }

            override fun onError() {
                fullscreenImageProgress.visibility = View.GONE
                Snackbar.make(fullscreenImageRoot, "Cannot show this image", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Close", { finish() })
                        .show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        } else
            return super.onOptionsItemSelected(item)
    }
}
