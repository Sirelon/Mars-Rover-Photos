package com.sirelon.marsroverphotos.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.view.MenuItemCompat
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.ShareActionProvider
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.extensions.showAppSettings
import com.sirelon.marsroverphotos.extensions.showSnackBar
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.widget.ViewsPagerAdapter
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_image.*
import kotlinx.android.synthetic.main.view_image.view.*


class ImageActivity : RxActivity() {

    companion object {
        val EXTRA_PHOTO = ".extraPhoto"
        val EXTRA_FILTER_BY_CAMERA = ".extraCameraFilterEnable"
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
        val shareIntent: Intent = Intent(Intent.ACTION_SEND)
        val shareText =
            "Take a look what I found on Mars ${marsPhoto.imageUrl} with this app \n\n$appUrl"
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
    }

    private val appUrl by lazy {
        "https://play.google.com/store/apps/details?id=" + packageName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_image)

        marsPhoto = intent.getParcelableExtra<MarsPhoto>(EXTRA_PHOTO)

        // Configure Ad
        AdvertisingObjectFactory.getAdvertisingDelegate()
                .loadAd(adViewBanner)

        val cameraFilterEnable = intent.getBooleanExtra(EXTRA_FILTER_BY_CAMERA, false)
        title = "Mars photo"

        if (dataManager.lastPhotosRequest == null) {
            showStandaloneImage()
        } else {
            val subscribe = dataManager.lastPhotosRequest!!.flatMapIterable { it }
                    .compose {
                        // Filter by cameras
                        if (cameraFilterEnable) it.filter { it.camera.id == marsPhoto.camera.id }
                        else it
                    }
                    .toList()
                    .subscribe({
                                   val pagerAdapter = ViewsPagerAdapter(it)
                                   imagePager.adapter = pagerAdapter
                                   pagerAdapter.scaleCallback = {
                                       if (!scaleWasSet) dataManager.updatePhotoScaleCounter(
                                               marsPhoto)

                                       scaleWasSet = true
                                   }
                                   it?.let {
                                       val index = it.indexOf(marsPhoto)
                                       imagePager.currentItem = index
                                   }
                                   imagePager.addOnPageChangeListener(object :
                                                                          ViewPager.SimpleOnPageChangeListener() {
                                       override fun onPageSelected(position: Int) {
                                           // Set the marsPhoto as current
                                           marsPhoto = it[position]
                                           scaleWasSet = false
                                           dataManager.updatePhotoSeenCounter(marsPhoto)
                                       }
                                   })
                               }, {
                                   it.printStackTrace()
                                   // Show Standalone Image
                                   showStandaloneImage()
                               })

            subscriptions.add(subscribe)
        }
    }

    private fun showStandaloneImage() {
        fullscreenImageRoot.removeView(imagePager)

        val imageRoot = fullscreenImageRoot.inflate(R.layout.view_image, false)

        fullscreenImageRoot.addView(imageRoot)

        val photoViewAttacher = PhotoViewAttacher(imageRoot.fullscreenImage)

        Picasso.with(this)
                .load(marsPhoto.imageUrl)
                .tag(marsPhoto.id)
                .into(imageRoot.fullscreenImage, object : Callback {
                    override fun onSuccess() {
                        imageRoot.fullscreenImageProgress.visibility = View.GONE
                        photoViewAttacher.update()
                    }

                    override fun onError() {
                        imageRoot.fullscreenImageProgress.visibility = View.GONE
                        showSnackBarError()
                    }
                })
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item?.itemId == R.id.menu_item_save) {
            saveImageToGallery()
            return true
        } else return super.onOptionsItemSelected(item)
    }

    private fun saveImageToGallery() {

        val subscribe = RxPermissions(this)
                // Request permission for saving file.
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                // filter not granted permission
                .filter {
                    //  If permission not granted and shouldShowRequestPermissionRationale = show explain dialog. else show snackbar with gotosettings
                    if (!it) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                                                                                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        ) showExplainDialog()
                        else {
                            showSnackBarOnSettings()
                        }
                    }
                    it
                }
                // Get Bitmap on background
                .observeOn(Schedulers.io())
                .map {
                    Picasso.with(this)
                            .load(marsPhoto.imageUrl)
                            .get()
                }
                // Save bitmap to gallery
                .map {
                    MediaStore.Images.Media.insertImage(contentResolver, it,
                                                        "mars_photo_${marsPhoto.id}",
                                                        "Photo saved from $appUrl")
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

    private fun ImageActivity.showSnackBarError() {
        fullscreenImageRoot.showSnackBar("Cannot show this image", "Close",
                                         View.OnClickListener { finish() },
                                         Snackbar.LENGTH_INDEFINITE)
    }

    private fun showSnackBarOnSettings() {
        fullscreenImageRoot.showSnackBar(
                "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings",
                "Go to Settings", View.OnClickListener { showAppSettings() })
    }

    private fun showSnackBarOnSaved(imagePath: String?) {
        fullscreenImageRoot.showSnackBar(msg = "File was saved on path $imagePath",
                                         actionTxt = "View", actionCallback = View.OnClickListener {
            val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imagePath))
            startActivity(openIntent)
        }, duration = Snackbar.LENGTH_INDEFINITE)
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
                        "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission.")
                .setPositiveButton("Ok") { p0, p1 -> saveImageToGallery() }
                .setNegativeButton("No", null)
                .show()

    }
}
