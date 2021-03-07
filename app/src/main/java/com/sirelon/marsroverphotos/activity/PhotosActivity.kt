package com.sirelon.marsroverphotos.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.util.Pair
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.AdsDelegateAdapter
import com.sirelon.marsroverphotos.adapter.MarsPhotosDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.extensions.logD
import com.sirelon.marsroverphotos.feature.advertising.AdvertisingObjectFactory
import com.sirelon.marsroverphotos.feature.photos.PhotosViewModel
import com.sirelon.marsroverphotos.models.OnModelChooseListener
import com.sirelon.marsroverphotos.models.PhotosQueryRequest
import com.sirelon.marsroverphotos.models.Rover
import com.sirelon.marsroverphotos.models.RoverDateUtil
import com.sirelon.marsroverphotos.models.ViewType
import com.sirelon.marsroverphotos.storage.MarsImage
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

class PhotosActivity : RxActivity(), OnModelChooseListener<MarsImage> {

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
//    private lateinit var queryRequest: PhotosQueryRequest

    private lateinit var rover: Rover
    private lateinit var dateUtil: RoverDateUtil
    private var filteredCamera: String? = null
    private var camerasList = emptyList<String>()

    private lateinit var actionRandom: FloatingActionButton
    private lateinit var photos_list: RecyclerView
    private lateinit var no_data_view: View
    private lateinit var photosCamera: TextView

    private val viewModel: PhotosViewModel by viewModels()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        outState.putParcelable(EXTRA_QUERY_REQUEST, queryRequest)
        outState.putParcelable(EXTRA_ROVER, rover)
    }

    override fun onModelChoose(model: MarsImage, vararg sharedElements: Pair<View, String>) {
        val photos = adapter.getData().filterIsInstance<MarsImage>()
        GlobalScope.launch {
            dataManager.cacheImages(photos)
        }

        val ids = photos.map { it.id }

        // Enable camera filter if the same camera was choose.
        // If all camera choosed then no need to filtering
        val cameraFilter = filteredCamera != null
        val intent = ImageActivity.createIntent(this, model.id, ids, cameraFilter)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photos)
        actionRandom = findViewById(R.id.actionRandom)
        no_data_view = findViewById<View>(R.id.no_data_view)
        val noData = findViewById<TextView>(R.id.title)
        noData.text = "No data here :("
        no_data_view.setOnClickListener {
            dataManager.trackClick("refresh_no_data")
            actionRandom.callOnClick()
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_QUERY_REQUEST)) {
            // Activity after savedInstance
            rover = savedInstanceState.getParcelable(EXTRA_ROVER)!!
//            queryRequest = savedInstanceState.getParcelable(EXTRA_QUERY_REQUEST)!!
        } else {
            val parcelableExtra = intent.getParcelableExtra<Rover>(EXTRA_ROVER)
            if (parcelableExtra == null) {
                finish()
                return
            }
            // New Activity
            rover = parcelableExtra

//            queryRequest = randomPhotosQueryRequest()
        }
        dateUtil = RoverDateUtil(rover)

        viewModel.setRoverId(rover.id)

        // Set Toolbar title
        title = "${rover.name}'s photos"

        photosCamera = findViewById(R.id.photosCamera)
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

        photos_list = findViewById<RecyclerView>(R.id.photos_list)
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
//            queryRequest = randomPhotosQueryRequest()
            updateDateSolChoose()
            updateDateEearthChoose(dateUtil.dateFromSol(viewModel.getSol()))

            adapter.clearAll()
//            loadFreshData()
            viewModel.randomize()
        }

        viewModel.photosFlow
            .onEach {
                it.logD()
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
            .launchIn(lifecycleScope)

//        loadFreshData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        rover = intent.getParcelableExtra(EXTRA_ROVER)!!

//        queryRequest = randomPhotosQueryRequest()
    }

    private fun loadFreshData() {
//        viewModel.setPhotosQuery(queryRequest)
    }

    private fun updateCameraFilter(photos: List<ViewType>) {
        photosCamera.visibility = View.GONE
        camerasList = photos
            .asSequence()
            .filterIsInstance<MarsImage>()
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
            .filterIsInstance<MarsImage>()
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

        val time = dateUtil.dateFromSol(viewModel.getSol())
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

        findViewById<View>(R.id.dateEarthChoose).setOnClickListener {
            dataManager.trackClick("choose_earth")
            // UPDATE TIME
            val timeFromSol = dateUtil.dateFromSol(viewModel.getSol())

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

        val solInput = dialogView.findViewById<EditText>(R.id.solInput)
        val solSeekBar = dialogView.findViewById<SeekBar>(R.id.solSeekBar)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Ok") { _, _ ->
                val sol = solInput.text.toString().toLongOrNull()
                sol ?: return@setPositiveButton
                updateDateEearthChoose(dateUtil.dateFromSol(sol))
                loadDataBySol(sol)
            }.create()

        updateDateSolChoose()
        findViewById<View>(R.id.dateSolChoose).setOnClickListener {
            dataManager.trackClick("choose_sol")
            // Update views
            solSeekBar.max = rover.maxSol.toInt()
            solSeekBar.progress = viewModel.getSol().toInt()
            val solStr = viewModel.getSol().toString()
            solInput.setText(solStr)
            solInput.setSelection(solStr.length)
            // Show dialog
            dialog.show()
        }

        val changeSubscription = Observable.create<CharSequence> {
            // Some setup for seek and edittext
            solInput.doOnTextChanged { text, _, _, _ ->
                it.onNext(text ?: "")
            }
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
                    solInput.setText("${rover.maxSol}")
                    false
                } else {
                    true
                }
            }
            .retry()
            .subscribe({ solSeekBar.progress = it }, { it.printStackTrace() })

        subscriptions.add(changeSubscription)

        solSeekBar.setOnSeekBarChangeListener(object :
                                                  SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                @Suppress("NAME_SHADOWING")
                var progress = progress
                if (progress < 0) progress = 1
                solInput.setText("$progress")
                solInput.setSelection(solInput.text.length)
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateDateSolChoose() {
        val dateSolChoose = findViewById<TextView>(R.id.dateSolChoose)
        dateSolChoose.text = "Sol date: ${viewModel.getSol()}"
    }

    @SuppressLint("SetTextI18n")
    private fun updateDateEearthChoose(time: Long) {
        val dateEarthChoose = findViewById<TextView>(R.id.dateEarthChoose)
        dateEarthChoose.text = "Earth date: ${dateUtil.parseTime(time)}"
    }

    private fun loadDataBySol(sol: Long) {

        viewModel.loadBySol(sol)

        // TODO:
//        if (sol == queryRequest.sol) return
//
//        queryRequest = queryRequest.copy(sol = sol)
        updateDateSolChoose()
//        // Clear adapter
//        adapter.clearAll()
//        loadFreshData()
    }
}
