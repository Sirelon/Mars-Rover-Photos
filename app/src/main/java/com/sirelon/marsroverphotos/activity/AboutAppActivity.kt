package com.sirelon.marsroverphotos.activity

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem
import com.sirelon.marsroverphotos.BuildConfig
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.extensions.spannable
import kotlinx.android.synthetic.main.activity_about_app.*
import java.util.*

class AboutAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        about_rights.text = getString(R.string.all_rights_reserved_fmt, Calendar.getInstance().get(Calendar.YEAR))

        about_rate_action.setOnClickListener {
            val uri = Uri.parse("market://details?id=" + packageName)
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            startActivity(goToMarket)
        }

        about_app_version.text = spannable {
            typeface(Typeface.BOLD) {
                +"Version: "
            }
            +BuildConfig.VERSION_NAME
        }.toCharSequence()

        about_email.text = spannable {
            typeface(Typeface.BOLD) {
                +"Email: "
            }
            +"sasha.sirelon@gmail.com"
        }.toCharSequence()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        } else return super.onOptionsItemSelected(item)
    }
}
