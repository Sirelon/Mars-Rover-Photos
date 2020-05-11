package com.sirelon.marsroverphotos.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuItemCompat
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.loadImage
import com.sirelon.marsroverphotos.extensions.showAppSettings
import com.sirelon.marsroverphotos.extensions.showSnackBar
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.widget.ViewsPagerAdapter
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_image.*
import kotlinx.android.synthetic.main.view_image.view.*
import kotlinx.android.synthetic.main.view_native_adview.*
import kotlin.math.abs
import kotlin.math.min


class ImageActivity : RxActivity() {

    companion object {
        const val EXTRA_PHOTO = ".extraPhoto"
        const val EXTRA_FILTER_BY_CAMERA = ".extraCameraFilterEnable"
        fun createIntent(context: Context, photo: MarsPhoto, cameraFilterEnable: Boolean): Intent {
            val intent = Intent(context, ImageActivity::class.java)
            intent.putExtra(EXTRA_PHOTO, photo)
            intent.putExtra(EXTRA_FILTER_BY_CAMERA, cameraFilterEnable)
            return intent
        }
    }

    private lateinit var marsPhoto: MarsPhoto

    private var scaleWasSet = false

    private val shareIntent by lazy {
        val shareIntent = Intent(Intent.ACTION_SEND)
        val shareText =
            "Take a look what I found on Mars ${marsPhoto.imageUrl} with this app \n\n$appUrl"
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
    }

    private val appUrl by lazy {
        "https://play.google.com/store/apps/details?id=$packageName"
    }

    private lateinit var rootForDrag: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image)

        val parcelableExtra = intent.getParcelableExtra<MarsPhoto?>(EXTRA_PHOTO)
        if (parcelableExtra == null) {
            finish()
            return
        }
        marsPhoto = parcelableExtra

        // Configure Ad
        AdvertisingObjectFactory.getAdvertisingDelegate()
            .loadAd(adViewBanner)

        val cameraFilterEnable = intent.getBooleanExtra(EXTRA_FILTER_BY_CAMERA, false)
        title = "Mars photo"
        rootForDrag = imagePager
        if (dataManager.lastPhotosRequest == null) {
            showStandaloneImage()
        } else {
            val subscribe = dataManager.lastPhotosRequest!!.flatMapIterable { it }
                .compose {
                    // Filter by cameras
                    if (cameraFilterEnable) it.filter { it.camera?.id == marsPhoto.camera?.id }
                    else it
                }
                .toList()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(::onCachePhotosAvailable) {
                    it.printStackTrace()
                    // Show Standalone Image
                    showStandaloneImage()
                }

            subscriptions.add(subscribe)
        }

        val dismissPathLength= resources.getDimensionPixelSize(R.dimen.dismiss_path_length)
        imageDragLayout.setOnDragListener { dy ->
            val processedAlpha = 1 - min(abs(dy / (3 * dismissPathLength)), 1f)
//            backgroundColorView.alpha = processedAlpha
            rootForDrag.alpha = processedAlpha
            rootForDrag.translationY = -dy
        }

        imageDragLayout.setOnReleaseDragListener { dy ->
            if (abs(dy) > dismissPathLength) {
                rootForDrag.visibility = View.GONE
                finishAfterTransition()
            } else {
//                backgroundColorView.alpha = 1f
                rootForDrag.alpha = 1f
                rootForDrag.translationY = 0f
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_image, menu)
        val menuItemShare = menu?.findItem(R.id.menu_item_share)
        val shareActionProvider = MenuItemCompat.getActionProvider(menuItemShare)
        if (shareActionProvider is ShareActionProvider) {
            shareActionProvider.setShareIntent(shareIntent)
            shareActionProvider.setOnShareTargetSelectedListener { _, intent ->
                dataManager.updatePhotoShareCounter(marsPhoto, intent.`package`)
                false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when {
        item?.itemId == R.id.menu_item_save -> true.apply { saveImageToGallery() }
        else -> super.onOptionsItemSelected(item)
    }

    private fun onCachePhotosAvailable(it: List<MarsPhoto>) {
        val pagerAdapter = ViewsPagerAdapter(it)
        imagePager.adapter = pagerAdapter
        pagerAdapter.scaleCallback = {
            if (!scaleWasSet) dataManager.updatePhotoScaleCounter(marsPhoto)

            scaleWasSet = true
        }

        val index = it.indexOf(marsPhoto)
        imagePager.currentItem = index

        imagePager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                // Set the marsPhoto as current
                marsPhoto = it[position]
                scaleWasSet = false
                dataManager.updatePhotoSeenCounter(marsPhoto)
            }
        })
    }

    private fun showStandaloneImage() {
        fullscreenImageRoot.removeView(imagePager)

        val imageRoot = fullscreenImageRoot.inflate(R.layout.view_image, false)
        rootForDrag= imageRoot
        fullscreenImageRoot.addView(imageRoot)
        imageRoot.fullscreenImage.loadImage(marsPhoto.imageUrl, false)
    }

    private fun saveImageToGallery() {
        val subscribe = RxPermissions(this)
            // Request permission for saving file.
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            // filter not granted permission
            .filter {
                //  If permission not granted and shouldShowRequestPermissionRationale = show explain dialog. else show snackbar with gotosettings
                if (!it) {
                    permissionNotGrant()
                }
                it
            }
            // Get Bitmap on background
            .observeOn(Schedulers.io())
            .map {
                Glide.with(this).asBitmap().load(marsPhoto.imageUrl).submit().get()
            }
            // Save bitmap to gallery
            .map {
                MediaStore.Images.Media.insertImage(
                    contentResolver, it,
                    "mars_photo_${marsPhoto.id}",
                    "Photo saved from $appUrl"
                )
            }
            // Send broadcast for updating gallery
            .doOnNext {
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(it)))
            }
            // Update counter for save
            .doOnNext { dataManager.updatePhotoSaveCounter(marsPhoto) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::showSnackBarOnSaved, ::onErrorOccurred)

        subscriptions.add(subscribe)
    }

    private fun permissionNotGrant() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            showExplainDialog()
        } else {
            showSnackBarOnSettings()
        }
    }

    private fun ImageActivity.showSnackBarError() {
        fullscreenImageRoot.showSnackBar(
            "Cannot show this image", "Close",
            View.OnClickListener { finish() },
            Snackbar.LENGTH_INDEFINITE
        )
    }

    private fun showSnackBarOnSettings() {
        fullscreenImageRoot.showSnackBar(
            "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings",
            "Go to Settings", View.OnClickListener { showAppSettings() })
    }

    private fun showSnackBarOnSaved(imagePath: String?) {
        fullscreenImageRoot.showSnackBar(
            msg = "File was saved on path $imagePath",
            actionTxt = "View", actionCallback = View.OnClickListener {
                val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imagePath))
                startActivity(openIntent)
            }, duration = Snackbar.LENGTH_SHORT
        )
    }

    private fun onErrorOccurred(it: Throwable) {
        it.printStackTrace()
        Toast.makeText(this, "Error occured ${it.message}", Toast.LENGTH_SHORT)
            .show()
    }

    private fun showExplainDialog() {
        AlertDialog.Builder(this)
            .setTitle("Alert")
            .setMessage(
                "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission."
            )
            .setPositiveButton("Ok") { _, _ -> saveImageToGallery() }
            .setNegativeButton("No", null)
            .show()

    }
}
