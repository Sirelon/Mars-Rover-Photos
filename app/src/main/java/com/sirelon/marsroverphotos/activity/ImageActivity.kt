package com.sirelon.marsroverphotos.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ShareActionProvider
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.extensions.showAppSettings
import com.sirelon.marsroverphotos.extensions.showSnackBar
import com.sirelon.marsroverphotos.feature.images.ImageViewModel
import com.sirelon.marsroverphotos.firebase.photos.FirestorePhotos
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.storage.MarsImage
import com.sirelon.marsroverphotos.utils.transformers.DepthPageTransformer
import com.sirelon.marsroverphotos.widget.ImagesPagerAdapter
import com.sirelon.marsroverphotos.widget.VerticalDragLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min


class ImageActivity : RxActivity() {

    companion object {
        const val EXTRA_SELECTED_ID = ".extraSelectedId"
        const val EXTRA_PHOTO_IDS = ".extraPhotosIds"
        const val EXTRA_FILTER_BY_CAMERA = ".extraCameraFilterEnable"

        fun createIntent(
            context: Context,
            selectedId: String,
            list: List<String>,
            cameraFilterEnable: Boolean
        ): Intent {
            val intent = Intent(context, ImageActivity::class.java)
            intent.putStringArrayListExtra(EXTRA_PHOTO_IDS, ArrayList(list))
            intent.putExtra(EXTRA_FILTER_BY_CAMERA, cameraFilterEnable)
            intent.putExtra(EXTRA_SELECTED_ID, selectedId)
            return intent
        }
    }

    private var marsPhoto: MarsImage? = null

    private var scaleWasSet = false

    private val imagesViewModel: ImageViewModel by viewModels()

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val shareIntent by lazy {
        val shareIntent = Intent(Intent.ACTION_SEND)
        val shareText =
            "Take a look what I found on Mars ${marsPhoto?.imageUrl} with this app \n\n$appUrl"
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

        val toolbar = findViewById<Toolbar>(R.id.imageToolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    marsPhoto?.toMarsPhoto()?.let { savePhotoToLocal(it) }
                } else {
                    permissionNotGrant()
                }
            }

        val ids = intent.getStringArrayListExtra(EXTRA_PHOTO_IDS)

        if (ids == null) {
            finish()
            return
        }

        imagesViewModel.setIdsToShow(ids)

        val imagePager = findViewById<ViewPager2>(R.id.imagePager)
        val cameraFilterEnable = intent.getBooleanExtra(EXTRA_FILTER_BY_CAMERA, false)
        title = "Mars photo"
        rootForDrag = imagePager

        val adapter = ImagesPagerAdapter(scaleCallback = {
            if (!scaleWasSet) marsPhoto?.toMarsPhoto()
                ?.let(dataManager::updatePhotoScaleCounter)

            scaleWasSet = true
        }, favoriteCallback = {
            imagesViewModel.updateFavorite(it)
        })

//        imagePager.setPageTransformer(DepthPageTransformer())

        // TODO: //                    // Filter by cameras
        ////                    if (cameraFilterEnable) it.filter { it.camera?.id == marsPhoto.camera?.id }

        imagePager.adapter = adapter
        imagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // Set the marsPhoto as current
                marsPhoto = adapter.currentList[position]
                scaleWasSet = false
                marsPhoto?.toMarsPhoto()?.let(dataManager::updatePhotoSeenCounter)
            }
        })

        val selectedId = intent.getStringExtra(EXTRA_SELECTED_ID)
        var firstLoad = true
        imagesViewModel.imagesLiveData.observe(this) { list ->
            Log.e("Sirelon", "OBSERVEEEE")
            list.logD()
            adapter.submitList(list) {
                if (firstLoad) {

                    val selectItem = list.indexOfFirst { it.id == selectedId }

                    imagePager.setCurrentItem(selectItem, false)
                    firstLoad = false
                }
            }
        }

        val imageDragLayout = findViewById<VerticalDragLayout>(R.id.imageDragLayout)
        val dismissPathLength = resources.getDimensionPixelSize(R.dimen.dismiss_path_length)
        imageDragLayout.setOnDragListener { dy ->
            val processedAlpha = 1 - min(abs(dy / (3 * dismissPathLength)), 1f)
//            backgroundColorView.alpha = processedAlpha
            rootForDrag.alpha = processedAlpha
            rootForDrag.translationY = -dy
        }

        imageDragLayout.setOnReleaseDragListener { dy ->
            if (abs(dy) > dismissPathLength) {
                rootForDrag.visibility = View.GONE
                ActivityCompat.finishAfterTransition(this)
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

                marsPhoto?.toMarsPhoto()?.let {

                    if (BuildConfig.DEBUG) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            FirestorePhotos().removePhoto(it)
                        }
                    } else {
                        dataManager.updatePhotoShareCounter(it, intent.`package`)
                    }
                }
                false
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_item_save -> true.apply { saveImageToGallery() }
        else -> super.onOptionsItemSelected(item)
    }

    private fun saveImageToGallery() {
        val marsPhoto = marsPhoto?.toMarsPhoto() ?: return

        // Do some stuff with photo at debug mode
        if (BuildConfig.DEBUG) {
            lifecycleScope.launch(Dispatchers.IO) {
                FirestorePhotos().makeItPopular(marsPhoto)
            }
            return
        }

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val grated =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (grated) {
            // blabla
            savePhotoToLocal(marsPhoto)
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun savePhotoToLocal(photo: MarsPhoto) {
        lifecycleScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                val bitmap =
                    Glide.with(this@ImageActivity).asBitmap().load(photo.imageUrl).submit().get()
                val localUrl = MediaStore.Images.Media.insertImage(
                    contentResolver, bitmap,
                    "mars_photo_${photo.id}",
                    "Photo saved from $appUrl"
                )
                sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(localUrl)))
//                dataManager.updatePhotoSaveCounter(photo)
                // Update counter for save
                localUrl
            }.onSuccess {
                withContext(Dispatchers.Main) {
                    showSnackBarOnSaved(it)
                }
            }.onFailure {
                withContext(Dispatchers.Main) {
                    onErrorOccurred(it)
                }
            }
        }
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

    private fun showSnackBarOnSettings() {
        val fullscreenImageRoot = findViewById<View>(R.id.fullscreenImageRoot)
        fullscreenImageRoot.showSnackBar(
            "Without this permission I cannot save this nice photo to your gallery. If you want to save image please give permission in settings",
            "Go to Settings", View.OnClickListener { showAppSettings() })
    }

    private fun showSnackBarOnSaved(imagePath: String?) {
        val fullscreenImageRoot = findViewById<View>(R.id.fullscreenImageRoot)
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
