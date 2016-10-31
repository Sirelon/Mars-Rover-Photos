package com.sirelon.marsroverphotos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import com.sirelon.marsroverphotos.models.MarsPhoto
import kotlinx.android.synthetic.main.activity_image.*;

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

        fullscreenImage.loadImage(marsPhoto.imageUrl)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        } else
            return super.onOptionsItemSelected(item)
    }
}
