package com.sirelon.marsroverphotos.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.ShareActionProvider
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.showAppSettings
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_image.*
import uk.co.senab.photoview.PhotoViewAttacher

class ImageActivity : RxActivity() {

    companion object {
        val EXTRA_PHOTO = ".extraPhoto"
        fun createIntent(context: Context, photo: MarsPhoto): Intent {
            val intent = Intent(context, ImageActivity::class.java)
            intent.putExtra(EXTRA_PHOTO, photo)
            return intent
        }
    }

    private lateinit var marsPhoto: MarsPhoto

    private val shareIntent by lazy {
        val shareIntent: Intent = Intent(Intent.ACTION_SEND)
        val shareText = "Take a look what I found on Mars ${marsPhoto.imageUrl} with this app \n\n$appUrl"
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
    }

    private val appUrl by lazy {
        "https://play.google.com/store/apps/details?id=" + packageName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataManager.lastPhotosRequest?.subscribe({ Log.w("Sirelon", "ONSUBSCRTIBE IMAGE + $it") }, { it.printStackTrace() })

        setContentView(R.layout.activity_image)

        marsPhoto = intent.getParcelableExtra<MarsPhoto>(EXTRA_PHOTO)

//        Photo id ${marsPhoto.id}.
        title = "Mars photo"

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_image, menu)
        val shareActionProvider = MenuItemCompat.getActionProvider(menu?.findItem(R.id.menu_item_share)) as ShareActionProvider

        shareActionProvider.setShareIntent(shareIntent)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        } else if (item?.itemId == R.id.menu_item_save) {
            saveImageToGallery()
            return true
        } else
            return super.onOptionsItemSelected(item)
    }

    private fun saveImageToGallery() {

        RxPermissions.getInstance(this)
                // Request permission for saving file.
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                // filter not granted permission
                .filter {
//                  If permission not granted and shouldShowRequestPermissionRationale = show explain dialog. else show snackbar with gotosettings
                    if (!it) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) showExplainDialog()
                        else {
                            Snackbar.make(fullscreenImageRoot, "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings", Snackbar.LENGTH_LONG)
                                    .setAction("Go to Settings", { showAppSettings() })
                                    .show()
                        }
                    }
                    it
                }
                // Get Bitmap on background
                .observeOn(Schedulers.io())
                .map { Picasso.with(this).load(marsPhoto.imageUrl).get() }
                // Save bitmap to gallery
                .map { MediaStore.Images.Media.insertImage(contentResolver, it, "mars_photo_${marsPhoto.id}", "Photo saved from $appUrl") }
                // Send broadcast for updating gallery
                .map {
                    sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(it)))
                    it
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    imagePath ->
                    Snackbar.make(fullscreenImageRoot, "File was saved on path $imagePath", Snackbar.LENGTH_INDEFINITE)
                            .setAction("View", {
                                val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(imagePath))
                                startActivity(openIntent)
                            })
                            .show()
                }, {
                    it.printStackTrace()
                    Toast.makeText(this, "Error occured ${it.message}", Toast.LENGTH_SHORT).show()
                })

    }

    private fun showExplainDialog() {
        val dialog = AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage("Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission.")
                .setPositiveButton("Ok") { p0, p1 -> saveImageToGallery() }
                .setNegativeButton("No", null)
                .show()

    }
}
