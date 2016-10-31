package com.sirelon.marsroverphotos

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_rovers.*

class RoversActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rovers)

        roversList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RoversActivity)
        }
    }
}
