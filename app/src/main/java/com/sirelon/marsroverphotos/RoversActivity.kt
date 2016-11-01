package com.sirelon.marsroverphotos

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.RoversDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.ViewType
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_rovers.*

class RoversActivity : RxActivity(), OnModelChooseListener {

    override fun onModelChoose(model: ViewType) {
        val queryRequest = PhotosQueryRequest((model as Rover).name, 1000, null)
        startActivity(PhotosActivity.createIntent(this, model as Rover))
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
                .subscribe({adapter.addOrReplace(it)}, Throwable::printStackTrace)

        subscriptions.add(subscription)
    }
}
