package com.sirelon.marsroverphotos.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.format.Formatter
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.cache.DiskCache
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.spannable
import kotlinx.android.synthetic.main.activity_about_app.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class AboutAppActivity : AppCompatActivity() {

    private val tracker = RoverApplication.APP.tracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        about_rights.text =
            getString(R.string.all_rights_reserved_fmt, Calendar.getInstance().get(Calendar.YEAR))

        about_rate_action.setOnClickListener {
            tracker.trackClick("goToMarket")
            goToMarket()
        }

        about_app_version.text = spannable {
            tracker.trackClick("About App")
            typeface(Typeface.BOLD) {
                +"Version: "
            }
            +BuildConfig.VERSION_NAME
        }.toCharSequence()

        about_email.text = spannable {
            tracker.trackClick("Email")
            typeface(Typeface.BOLD) {
                +"Email: "
            }
            +"sasha.sirelon@gmail.com"
        }.toCharSequence()

        findViewById<View>(R.id.clearCacheAction).setOnClickListener {
            tracker.trackClick("Clear cache")
            clearCache()
        }
    }

    private fun clearCache() {
        val ctx = application
        lifecycleScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, throwable -> throwable.printStackTrace() }) {
            val cacheFile = File(cacheDir, DiskCache.Factory.DEFAULT_DISK_CACHE_DIR)
            val size = calculateSize(cacheFile)

            Glide.get(ctx).clearDiskCache()

            val sizeStr = Formatter.formatFileSize(ctx, size)
            withContext(Dispatchers.Main) {
                Toast.makeText(ctx, "Cleared $sizeStr", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun calculateSize(dir: File?): Long {
        if (dir == null) return 0
        if (!dir.isDirectory) return dir.length()
        var result: Long = 0
        val children: Array<File>? = dir.listFiles()
        if (children != null) for (child in children) result += calculateSize(child)
        return result
    }

    private fun goToMarket() {
        val uri = Uri.parse("market://details?id=$packageName")
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else return super.onOptionsItemSelected(item)
    }
}
