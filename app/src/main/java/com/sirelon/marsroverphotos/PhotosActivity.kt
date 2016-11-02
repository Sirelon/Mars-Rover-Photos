package com.sirelon.marsroverphotos

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import com.sirelon.marsroverphotos.adapter.AdapterConstants
import com.sirelon.marsroverphotos.adapter.MarsPhotosDelegateAdapter
import com.sirelon.marsroverphotos.adapter.ViewTypeAdapter
import com.sirelon.marsroverphotos.models.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_photo_header.*
import kotlinx.android.synthetic.main.view_choose_sol.view.*
import java.text.SimpleDateFormat
import java.util.*

class PhotosActivity : RxActivity(), OnModelChooseListener {

    companion object {
        val EXTRA_ROVER = ".extraRover"

        fun createIntent(context: Context, rover: Rover): Intent {
            val intent = Intent(context, PhotosActivity::class.java)
            intent.putExtra(EXTRA_ROVER, rover)
            return intent
        }
    }

    override fun onModelChoose(model: ViewType) {
        if (model is MarsPhoto) {
            startActivity(ImageActivity.createIntent(this, model))
        }
    }

    private lateinit var queryRequest: PhotosQueryRequest
    private lateinit var rover: Rover

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rover = intent.getParcelableExtra<Rover>(EXTRA_ROVER)
        queryRequest = PhotosQueryRequest(rover.name, 1, null)

        initHeaderView()

        photosList.setHasFixedSize(true)

        photosList.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        val adapter = ViewTypeAdapter()
        adapter.addDelegateAdapter(AdapterConstants.MARS_PHOTO, MarsPhotosDelegateAdapter(this))

        photosList.adapter = adapter

        loadData()
    }

    private fun loadData() {
        val subscription = dataManager.getMarsPhotos(queryRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    (photosList.adapter as ViewTypeAdapter).addData(it)
                }, Throwable::printStackTrace)


        subscriptions.add(subscription)
    }

    private fun initHeaderView() {
        setupViewsForSetSol()

        setupViewsForEarthDate()
    }

    private fun setupViewsForEarthDate() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val timestampMax: Date?
        val timestampMin: Date?

        timestampMax = try {
            sdf.parse(rover.maxDate)
        } catch (e: Exception) {
            null
        }

        timestampMin = try {
            sdf.parse(rover.landingDate)
        } catch (e: Exception) {
            null
        }

        val calender = Calendar.getInstance(TimeZone.getDefault())
        calender.clear()
        calender.timeInMillis = timestampMax?.time ?: System.currentTimeMillis()

        val datePicker = DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener
                { datePicker, year, monthOfYear, dayOfMonth ->
                    calender.clear()
                    calender.set(year, monthOfYear, dayOfMonth)
                    val chooseDateMil = calender.timeInMillis
                    dateEarthChoose.text = sdf.format(chooseDateMil)

                    // + include end and curent days
                    val days = (chooseDateMil - timestampMin!!.time) / (1000 * 60 * 60 * 24) + 2

                    val sol = days / 1.0275
                    Log.d("Sirelon", "DEFERENCE BETWEEN DATES EARTH DAYS = $days SOL = $sol")
                    loadDataBySol(sol.toLong())
                },
                calender.get(Calendar.YEAR),
                calender.get(Calendar.MONTH),
                calender.get(Calendar.DAY_OF_MONTH))

        datePicker.datePicker.maxDate = timestampMax?.time ?: -1
        datePicker.datePicker.minDate = timestampMin?.time ?: -1

        dateEarthChoose.setOnClickListener {
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
                    val sol = dialogView.solInput.text.toString().toLong();
                    loadDataBySol(sol)
                }).create()

        dateSolChoose.text = "Sol date: ${queryRequest.sol}"
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

                override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
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

    private fun loadDataBySol(sol: Long) {
        queryRequest.sol = sol;
        subscriptions.clear()
        dateSolChoose.text = "Sol date: ${queryRequest.sol}"
        // Clear adapter
        (photosList.adapter as ViewTypeAdapter).clearAll()
        loadData()
    }

    private val photosList by lazy { photos_list }
}
