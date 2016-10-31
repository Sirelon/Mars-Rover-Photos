package com.sirelon.marsroverphotos

import android.content.Context
import android.content.Intent
import android.graphics.Matrix
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import com.sirelon.marsroverphotos.models.MarsPhoto
import kotlinx.android.synthetic.main.activity_image.*

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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGesture.onTouchEvent(event)
        return true
    }

    private val scaleGesture by lazy {
        ScaleGestureDetector(this, ScaleListener(fullscreenImage))
    }

    inner class ScaleListener(val imageView: ImageView) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private var scale = 1f
        private val matrix = Matrix()

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scale *= detector.scaleFactor
            scale = Math.max(0.1f, Math.min(scale, 5.0f))
            matrix.setScale(scale, scale)
            imageView.imageMatrix = matrix;

            return true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        } else
            return super.onOptionsItemSelected(item)
    }
}
