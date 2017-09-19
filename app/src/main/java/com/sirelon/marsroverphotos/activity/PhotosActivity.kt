package com.sirelon.marsroverphotos.activity

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import com.google.android.gms.ads.MobileAds
import com.sirelon.marsroverphotos.NoConnectionError
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.AdsDelegateAdapter
import com.sirelon.marsroverphotos.adapter.MarsPhotosDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.extensions.isConnected
import com.sirelon.marsroverphotos.extensions.random
import com.sirelon.marsroverphotos.models.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_photo_header.*
import kotlinx.android.synthetic.main.view_choose_sol.view.*
import java.util.*


class PhotosActivity : RxActivity(), OnModelChooseListener {

    companion object {
        const val EXTRA_ROVER = ".extraRover"
        val EXTRA_QUERY_REQUEST = ".extraQueryRequest"

        fun createIntent(context: Context, rover: Rover): Intent {
            val intent = Intent(context, PhotosActivity::class.java)
            intent.putExtra(EXTRA_ROVER, rover)
            return intent
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putParcelable(EXTRA_QUERY_REQUEST, queryRequest)
        outState?.putParcelable(EXTRA_ROVER, rover)
    }

    override fun onModelChoose(model: ViewType) {
        if (model is MarsPhoto) {
            // Enable camera filter if the same camera was choose.
            // If all camera choosed then no need to filtering
            val cameraFilter = photosCamera.selectedItemPosition != 0
            startActivity(ImageActivity.createIntent(this, model, cameraFilter))
        }
    }

    private val photosList by lazy { photos_list }

    private lateinit var queryRequest: PhotosQueryRequest
    private lateinit var rover: Rover
    private lateinit var dateUtil: RoverDateUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_QUERY_REQUEST)) {
            // Activity after savedInstance
            rover = savedInstanceState.getParcelable(EXTRA_ROVER)
            queryRequest = savedInstanceState.getParcelable(EXTRA_QUERY_REQUEST)
        } else {
            // New Activity
            rover = intent.getParcelableExtra(EXTRA_ROVER)

            queryRequest = randomPhotosQueryRequest()
        }
        dateUtil = RoverDateUtil(rover)

        // Set Toolbar title
        title = "${rover.name}'s photos"

        photosCamera.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

            override fun onItemSelected(p0: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterDataByCamera(position)
            }
        }

        initHeaderView()

        val adapter = ViewTypeAdapter()
        adapter.addDelegateAdapter(AdapterConstants.MARS_PHOTO, MarsPhotosDelegateAdapter(this))

        // Configurate ads
        MobileAds.initialize(this, getString(R.string.ad_application_id))
        adapter.addDelegateAdapter(AdapterConstants.ADVERTIZING, AdsDelegateAdapter())

        photosList.apply {
            setHasFixedSize(true)

            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

            this.adapter = adapter
        }

        actionRandom.setOnClickListener {
            queryRequest = randomPhotosQueryRequest()
            updateDateSolChoose()
            updateDateEearthChoose(dateUtil.dateFromSol(queryRequest.sol))

            (photosList.adapter as ViewTypeAdapter).clearAll()
            loadFreshData()
        }

        // if we recreate activity - we want to use cache, if available
        if (savedInstanceState != null)
            loadCacheOrRequest()
        else
            loadFreshData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        rover = intent.getParcelableExtra(EXTRA_ROVER)

        queryRequest = randomPhotosQueryRequest()
    }

    private fun randomPhotosQueryRequest() = PhotosQueryRequest(rover.name, 1.random(rover.maxSol.toInt()).toLong(), null)

    private fun loadCacheOrRequest() {
        // If we already have lastPhotosRequest - just use it, its returned to us cached results
        val observable: Observable<MutableList<out ViewType>?>
        if (dataManager.lastPhotosRequest != null)
            observable = dataManager.lastPhotosRequest!! as Observable<MutableList<out ViewType>?>
        else
            observable = photosObservable

        loadDataWithObservable(observable)
    }

    // Load data from given observable
    private fun loadDataWithObservable(observable: Observable<MutableList<out ViewType>?>) {
        subscriptions.clear()
        val subscription = observable.subscribe({
            it!!
            addAdToData(it as MutableList<ViewType>)
            (photosList.adapter as ViewTypeAdapter).addData(it)
            updateCameraFilter(it)
        }, Throwable::printStackTrace)

        subscriptions.add(subscription)
    }

    private fun loadFreshData() {
        loadDataWithObservable(photosObservable)
    }

    val photosObservable: Observable<MutableList<out ViewType>?> by lazy {
        Observable
                .just(isConnected())
                .switchMap {
                    if (it)
                        dataManager.loadMarsPhotos(queryRequest)
                    else
                        Observable.error { NoConnectionError() }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { (photosList.adapter as ViewTypeAdapter).stopLoading() }
                .doOnError(errorConsumer({ loadFreshData() }))
                .map { addAdToData(it) }
    }

    private fun addAdToData(dataList: MutableList<out ViewType>): MutableList<out ViewType> {
//        data as MutableList<ViewType>

        var step = 30

        val data: MutableList<ViewType> = dataList.toMutableList()

        if (step >= data.size / 2) {
            val advertizingItem = object : ViewType {
                override fun getViewType(): Int = AdapterConstants.ADVERTIZING
            }
            data.add(data.size, advertizingItem)
            return data
        }

        if (step >= data.size) {
            step = data.size / 2 + 1
        }

        IntProgression.fromClosedRange(step, data.size, step).map {
            val advertizingItem = object : ViewType {
                override fun getViewType(): Int = AdapterConstants.ADVERTIZING
            }
            data.add(it, advertizingItem)
        }
        return data
    }

    private fun updateCameraFilter(photos: List<ViewType>) {
        val cameraFilterSub = Observable
                .fromIterable(photos)
                .filter { it is MarsPhoto }
                .map { it as MarsPhoto }
                .map { it.camera }
                .distinct()
                .map { it.fullName }
                .toList()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    photosCamera.apply {
                        it.add(0, "All cameras")
                        val arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, it)
                        arrayAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
                        adapter = arrayAdapter
                    }
                }, Throwable::printStackTrace)

        subscriptions.add(cameraFilterSub)
    }

    private fun filterDataByCamera(position: Int) {

        var dataList = (photosList.adapter as ViewTypeAdapter).getSavedData()
        if (position == 0) {
            if (dataList == null)
                return

            (photosList.adapter as ViewTypeAdapter).clearAll()
            (photosList.adapter as ViewTypeAdapter).addData(dataList)
            return
        }

        if (dataList == null)
            dataList = (photosList.adapter as ViewTypeAdapter).getData()

        val cameraFullName: String? = photosCamera.adapter.getItem(position).toString()

        val filteredData = dataList.filter {
            if (it is MarsPhoto) {
                it.camera.fullName.equals(cameraFullName)
            } else
                false
        }

        // Add ad data to list
        val dataList1 = addAdToData(filteredData.toMutableList())

        (photosList.adapter as ViewTypeAdapter).applyFilteredData(dataList1)

    }

    private fun initHeaderView() {
        setupViewsForSetSol()

        setupViewsForEarthDate()
    }

    private fun setupViewsForEarthDate() {
        val calender = Calendar.getInstance(TimeZone.getDefault())
        calender.clear()

        val time = dateUtil.dateFromSol(queryRequest.sol)
        updateDateEearthChoose(time)

        calender.timeInMillis = time

        val datePicker = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener
                { datePicker, year, monthOfYear, dayOfMonth ->
                    calender.clear()
                    calender.set(year, monthOfYear, dayOfMonth)
                    val chooseDateMil = calender.timeInMillis
                    updateDateEearthChoose(chooseDateMil)
                    loadDataBySol(dateUtil.solFromDate(chooseDateMil))
                },
                calender.get(Calendar.YEAR),
                calender.get(Calendar.MONTH),
                calender.get(Calendar.DAY_OF_MONTH))

        datePicker.datePicker.maxDate = dateUtil.roverLastDate
        datePicker.datePicker.minDate = dateUtil.roverLandingDate
        dateEarthChoose.setOnClickListener {
            // UPDATE TIME
            val timeFromSol = dateUtil.dateFromSol(queryRequest.sol)

            calender.timeInMillis = timeFromSol

            datePicker.updateDate(
                    calender.get(Calendar.YEAR),
                    calender.get(Calendar.MONTH),
                    calender.get(Calendar.DAY_OF_MONTH))

            // Hide title. Need to set AFTER all
            datePicker.setTitle("")
            // SHOW DIALOG
            datePicker.show()
        }
    }

    private fun setupViewsForSetSol() {
        val dialogView = layoutInflater.inflate(R.layout.view_choose_sol, null, false)

        val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Ok", {
                    dialogInterface, i ->
                    val sol = dialogView.solInput.text.toString().toLong()
                    updateDateEearthChoose(dateUtil.dateFromSol(sol))
                    loadDataBySol(sol)
                }).create()

        updateDateSolChoose()
        dateSolChoose.setOnClickListener {
            // Update views
            dialogView.solSeekBar.max = rover.maxSol.toInt()
            dialogView.solSeekBar.progress = queryRequest.sol.toInt()
            val solStr = queryRequest.sol.toString()
            dialogView.solInput.setText(solStr)
            dialogView.solInput.setSelection(solStr.length)
            // Show dialog
            dialog.show()
        }

        val changeSubscription = Observable.create<CharSequence> {
            // Some setup for seek and edittext
            dialogView.solInput.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(p0: Editable?) {
                }

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun onTextChanged(text: CharSequence, p1: Int, p2: Int, p3: Int) {
                    it.onNext(text)
                }
            })
        }
                .filter { it.isNotEmpty() }
                .filter { TextUtils.isDigitsOnly(it) }
                .map { it.toString().toInt() }
                .filter {
                    if (it > rover.maxSol) {
                        Toast.makeText(this, "The max sol for ${rover.name}'s rover is ${rover.maxSol}", Toast.LENGTH_SHORT).show()
                        dialogView.solInput.setText("${rover.maxSol}")
                        false
                    } else {
                        true
                    }
                }
                .retry()
                .subscribe({ dialogView.solSeekBar.progress = it }, { it.printStackTrace() })

        subscriptions.add(changeSubscription)

        dialogView.solSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                @Suppress("NAME_SHADOWING")
                var progress = progress
                if (progress <= 0) progress = 1
                dialogView.solInput.setText("$progress")
                dialogView.solInput.setSelection(dialogView.solInput.text.length)
            }
        })
    }

    private fun updateDateSolChoose() {
        dateSolChoose.text = "Sol date: ${queryRequest.sol}"
    }

    private fun updateDateEearthChoose(time: Long) {
        dateEarthChoose.text = "Earth date: ${dateUtil.parseTime(time)}"
    }

    private fun loadDataBySol(sol: Long) {
        queryRequest.sol = sol
        updateDateSolChoose()
        // Clear adapter
        (photosList.adapter as ViewTypeAdapter).clearAll()
        loadFreshData()
    }
}
