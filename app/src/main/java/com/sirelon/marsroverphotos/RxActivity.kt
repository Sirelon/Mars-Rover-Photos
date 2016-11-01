package com.sirelon.marsroverphotos

import android.support.v7.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable

/**
 * @author romanishin
 * @since 01.11.16 on 11:18
 */
open class RxActivity : AppCompatActivity(){

    var subscriptions = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        subscriptions = CompositeDisposable()
    }

    override fun onPause() {
        super.onPause()
        subscriptions.clear()
    }

    val dataManager by lazy { DataManager() }
}