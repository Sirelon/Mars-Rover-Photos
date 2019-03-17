package com.sirelon.marsroverphotos.activity

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.sirelon.marsroverphotos.NoConnectionError
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.RoverApplication
import com.sirelon.marsroverphotos.extensions.isConnected
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_photos.*
import java.net.UnknownHostException

/**
 * @author romanishin
 * @since 01.11.16 on 11:18
 */
open class RxActivity : AppCompatActivity() {

    var subscriptions = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        subscriptions = CompositeDisposable()
    }

    override fun onPause() {
        super.onPause()
        subscriptions.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (item?.itemId == R.id.menu_item_about) {
//            FirebaseProvider.proideTestFirebase.deleteUnusedItems()
//            FirebaseProvider.proideTestFirebase.migrateToFirestore()
            startActivity(Intent(this, AboutAppActivity::class.java))
            return true
        } else
            return super.onOptionsItemSelected(item)
    }

    val dataManager by lazy { RoverApplication.APP.dataManger }

    fun errorConsumer(listener: () -> Unit): Consumer<in Throwable> {
        return Consumer {
            it.printStackTrace()
            if (isItNoConnection(it)) {
                showNoConnectionView(listener)
            } else {
                it.printStackTrace()
                Snackbar.make(
                    activity_main_root,
                    "Error occurred: ${it.message}",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Retry") {
                        subscriptions.clear()
                        listener()
                    }.show()
            }
        }
    }

    private fun isItNoConnection(it: Throwable?) =
        it is NoConnectionError || !isConnected() || it is UnknownHostException

    fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun showNoConnectionView(listener: () -> Unit) {
        for (i in 0..contentView.childCount) {
            contentView.getChildAt(i)?.visibility = View.GONE
        }
        contentView.addView(noConnectionView)
        noConnectionView.setOnClickListener {
            if (isConnected()) {
                contentView.removeView(noConnectionView)
                for (i in 0..contentView.childCount) {
                    contentView.getChildAt(i)?.visibility = View.VISIBLE
                }
                listener()
            }
        }
    }

    private val noConnectionView by lazy {
        layoutInflater.inflate(R.layout.view_no_connection, null)
    }

    private val contentView by lazy {
        findViewById<ViewGroup>(android.R.id.content)
    }
}