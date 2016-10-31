package com.sirelon.marsroverphotos

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.RoversDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.ViewType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_rovers.*

class RoversActivity : AppCompatActivity(), OnModelChooseListener {

    override fun onModelChoose(model: ViewType) {
        // TODO
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rovers)

        val adapter = ViewTypeAdapter(false)
        adapter.addDelegateAdapter(AdapterConstants.ROVER, RoversDelegateAdapter(this@RoversActivity))

        roversList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RoversActivity)
            this.adapter = adapter
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }

        val subscription = dataManager.getRovers()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ adapter.addData(it) }, Throwable::printStackTrace)
    }

    private val dataManager by lazy { DataManager() }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            val intent = Intent(this, PhotosActivity::class.java)
            startActivity(intent)
            return true
        } else
            return super.onOptionsItemSelected(item)
    }
}
