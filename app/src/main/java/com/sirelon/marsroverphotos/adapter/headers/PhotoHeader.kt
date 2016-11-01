package com.sirelon.marsroverphotos.adapter.headers

import android.graphics.Canvas
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.inflate

/**
 * @author romanishin
 * @since 01.11.16 on 14:51
 */
class PhotoHeader(recycler: RecyclerView, val headerId: Int) : RecyclerView.ItemDecoration() {

    private var headerView: View

    init {
        headerView = recycler.inflate(R.layout.item_photo_header)
//        Log.w("Sirelon", "HEADER WIDTH = ${headerView.width} Recycler WIDTH = ${recycler.measuredWidth}")
        headerView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))

        headerView.setOnClickListener {   Log.w("Sirelon", "ON CLICK OCCURED") }
    }

    override fun getItemOffsets(outRect: Rect?, view: View, parent: RecyclerView, state: RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)
        if (itemPosition == RecyclerView.NO_POSITION)
            return

//        if (headerId == parent.adapter.getItemViewType(itemPosition))
//            outRect?.top = 80

        if (itemPosition == 0 || itemPosition == 1)
            outRect?.top = headerView.measuredHeight
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        super.onDrawOver(c, parent, state)

        val childCount = parent.childCount
        if (childCount <= 0 || parent.adapter?.itemCount!! <= 0) {
            return
        }

        headerView.layout(parent.left, 0, parent.right, headerView.measuredHeight)

        for (i in 0..childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION)
                continue

            if (position == 0) {

                val transX = parent.x
                val transY = view.top

                c.save()
//                c.translate(0.toFloat(), 180.toFloat())
                headerView.draw(c)
                c.restore()
            }
        }
    }

    private fun isUnderHeader(itemPositition: Int): Boolean {
        if (itemPositition == 0)
            return true
        else
            return false
    }

}