package com.sirelon.marsroverphotos.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sirelon.marsroverphotos.NoConnectionError
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.AdsDelegateAdapter
import com.sirelon.marsroverphotos.adapter.MarsPhotosDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.extensions.isConnected
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.models.MarsPhoto
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.RoverDateUtil
import com.sirelon.marsroverphotos.models.ViewType
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_photos.*
import kotlinx.android.synthetic.main.item_photo_header.*
import kotlinx.android.synthetic.main.view_choose_sol.view.*
import kotlinx.android.synthetic.main.view_no_connection.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

class PhotosActivity : RxActivity(), OnModelChooseListener<MarsPhoto> {

    companion object {
        const val EXTRA_ROVER = ".extraRover"
        val EXTRA_QUERY_REQUEST = ".extraQueryRequest"

        fun createIntent(context: Context, rover: Rover): Intent {
            val intent = Intent(context, PhotosActivity::class.java)
            intent.putExtra(EXTRA_ROVER, rover)
            return intent
        }
    }

    private val advertisingDelegate = AdvertisingObjectFactory.getAdvertisingDelegate()

    private val adapter = ViewTypeAdapter()
    private lateinit var queryRequest: PhotosQueryRequest

    private lateinit var rover: Rover
    private lateinit var dateUtil: RoverDateUtil
    private var filteredCamera: String? = null
    private var camerasList = emptyList<String>()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(EXTRA_QUERY_REQUEST, queryRequest)
        outState.putParcelable(EXTRA_ROVER, rover)
    }

    override fun onModelChoose(model: MarsPhoto, vararg sharedElements: Pair<View, String>) {
        val photos = adapter.getData().filterIsInstance<MarsPhoto>()
        GlobalScope.launch {
            dataManager.cacheImages(photos)
        }


        val ids = photos.map { it.id.toInt() }

        // Enable camera filter if the same camera was choose.
        // If all camera choosed then no need to filtering
        val cameraFilter = filteredCamera != null
        val intent = ImageActivity.createIntent(this, model.id.toInt(), ids, cameraFilter)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos)

        no_data_view.title.text = "No data here :("
        no_data_view.setOnClickListener {
            dataManager.trackClick("refresh_no_data")
            actionRandom.callOnClick()
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_QUERY_REQUEST)) {
            // Activity after savedInstance
            rover = savedInstanceState.getParcelable(EXTRA_ROVER)!!
            queryRequest = savedInstanceState.getParcelable(EXTRA_QUERY_REQUEST)!!
        } else {
            val parcelableExtra = intent.getParcelableExtra<Rover>(EXTRA_ROVER)
            if (parcelableExtra == null) {
                finish()
                return
            }
            // New Activity
            rover = parcelableExtra

            queryRequest = randomPhotosQueryRequest()
        }
        dateUtil = RoverDateUtil(rover)

        // Set Toolbar title
        title = "${rover.name}'s photos"

        photosCamera.setOnClickListener {
            dataManager.trackClick("camera")
            AlertDialog.Builder(this)
                .setSingleChoiceItems(camerasList.toTypedArray(), 0) { dialog, position ->
                    filterDataByCamera(position)
                    dialog.dismiss()
                }
                .show()
        }

        initHeaderView()

        adapter.addDelegateAdapter(AdapterConstants.MARS_PHOTO, MarsPhotosDelegateAdapter(this))

        adapter.addDelegateAdapter(AdapterConstants.ADVERTIZING, AdsDelegateAdapter())

        photos_list.apply {
            setHasFixedSize(true)

            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

            this.adapter = this@PhotosActivity.adapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0 || dy < 0 && actionRandom.isShown) {
                        actionRandom.hide()
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        actionRandom.show()
                    }

                    super.onScrollStateChanged(recyclerView, newState)
                }
            })
        }

        actionRandom.setOnClickListener {
            dataManager.trackClick("refresh")
            queryRequest = randomPhotosQueryRequest()
            updateDateSolChoose()
            updateDateEearthChoose(dateUtil.dateFromSol(queryRequest.sol))

            adapter.clearAll()
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
        rover = intent.getParcelableExtra(EXTRA_ROVER)!!

        queryRequest = randomPhotosQueryRequest()
    }

    private fun randomPhotosQueryRequest(): PhotosQueryRequest {
        val sol = Random.nextLong(1L, rover.maxSol)
        return PhotosQueryRequest(rover.name, sol, null)
    }

    private fun loadCacheOrRequest() {
        // If we already have lastPhotosRequest - just use it, its returned to us cached results
        val observable: Observable<List<ViewType>?> =
            if (dataManager.lastPhotosRequest != null) dataManager.lastPhotosRequest!! as Observable<List<ViewType>?>
            else photosObservable

        loadDataWithObservable(observable)
    }

    // Load data from given observable
    private fun loadDataWithObservable(observable: Observable<List<ViewType>?>) {
        subscriptions.clear()
        val subscription = observable
            .onErrorReturnItem(emptyList())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (it.isNullOrEmpty()) {
                    photos_list.visibility = View.GONE
                    no_data_view.visibility = View.VISIBLE
                } else {
                    no_data_view.visibility = View.GONE
                    photos_list.visibility = View.VISIBLE
                    advertisingDelegate.integregrateAdToList(it)
//                    adapter.addData(it)
                    adapter.replaceData(it)
                    updateCameraFilter(it)
                }
            }

        subscriptions.add(subscription)
    }

    private fun loadFreshData() {
        loadDataWithObservable(photosObservable)
    }

    private val photosObservable: Observable<List<ViewType>?> by lazy {
        Observable
            .just(isConnected())
            .switchMap {
                if (it) dataManager.loadMarsPhotos(queryRequest)
                else Observable.error { NoConnectionError() }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { adapter.stopLoading() }
            .doOnError(errorConsumer { loadFreshData() })
            .map { advertisingDelegate.integregrateAdToList(it) }
    }

    private fun updateCameraFilter(photos: List<ViewType>) {
        photosCamera.visibility = View.GONE
        camerasList = photos
            .asSequence()
            .filterIsInstance<MarsPhoto>()
            .mapNotNull { it.camera }
            .map { it.fullName }
            .distinct()
            .toList()

        if (camerasList.isNotEmpty()) {
            photosCamera.visibility = View.VISIBLE
            camerasList = listOf("All cameras") + camerasList
            filteredCamera = camerasList.first()
            photosCamera.text = filteredCamera
        } else {
            photosCamera.visibility = View.GONE
        }
    }

    private fun filterDataByCamera(position: Int) {

        filteredCamera = camerasList.getOrNull(position) ?: return

        var dataList = adapter.getSavedData()
        if (position == 0) {
            if (dataList == null)
                return

            adapter.clearAll()
            adapter.addData(dataList)
            return
        }

        if (dataList == null)
            dataList = adapter.getData()

        val filteredData = dataList
            .filterIsInstance<MarsPhoto>()
            .filter { filteredCamera == it.camera?.fullName }

        // Add ad data to list
        val dataList1 = advertisingDelegate.integregrateAdToList(filteredData)

        adapter.applyFilteredData(dataList1)
        photosCamera.text = filteredCamera
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
            { _, year, monthOfYear, dayOfMonth ->
                calender.clear()
                calender.set(year, monthOfYear, dayOfMonth)
                val chooseDateMil = calender.timeInMillis
                updateDateEearthChoose(chooseDateMil)
                loadDataBySol(dateUtil.solFromDate(chooseDateMil))
            },
            calender.get(Calendar.YEAR),
            calender.get(Calendar.MONTH),
            calender.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.maxDate = dateUtil.roverLastDate
        datePicker.datePicker.minDate = dateUtil.roverLandingDate
        dateEarthChoose.setOnClickListener {
            dataManager.trackClick("choose_earth")
            // UPDATE TIME
            val timeFromSol = dateUtil.dateFromSol(queryRequest.sol)

            calender.timeInMillis = timeFromSol

            datePicker.updateDate(
                calender.get(Calendar.YEAR),
                calender.get(Calendar.MONTH),
                calender.get(Calendar.DAY_OF_MONTH)
            )

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
            .setPositiveButton("Ok") { _, _ ->
                val sol = dialogView.solInput.text.toString().toLongOrNull()
                sol ?: return@setPositiveButton
                updateDateEearthChoose(dateUtil.dateFromSol(sol))
                loadDataBySol(sol)
            }.create()

        updateDateSolChoose()
        dateSolChoose.setOnClickListener {
            dataManager.trackClick("choose_sol")
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
                    Toast.makeText(
                        this,
                        "The max sol for ${rover.name}'s rover is ${rover.maxSol}",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialogView.solInput.setText("${rover.maxSol}")
                    false
                } else {
                    true
                }
            }
            .retry()
            .subscribe({ dialogView.solSeekBar.progress = it }, { it.printStackTrace() })

        subscriptions.add(changeSubscription)

        dialogView.solSeekBar.setOnSeekBarChangeListener(object :
                                                             SeekBar.OnSeekBarChangeListener {
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

    @SuppressLint("SetTextI18n")
    private fun updateDateSolChoose() {
        dateSolChoose.text = "Sol date: ${queryRequest.sol}"
    }

    @SuppressLint("SetTextI18n")
    private fun updateDateEearthChoose(time: Long) {
        dateEarthChoose.text = "Earth date: ${dateUtil.parseTime(time)}"
    }

    private fun loadDataBySol(sol: Long) {
        queryRequest.sol = sol
        updateDateSolChoose()
        // Clear adapter
        adapter.clearAll()
        loadFreshData()
    }
}
