package com.sirelon.marsroverphotos.adapter.headers

import android.graphics.Canvas
import android.graphics.Rect
import androidx.recyclerview.widget.RecyclerView
import android.view.View

/**
 * @author romanishin
 * @since 01.11.16 on 14:51
 */
class PhotoHeader( var headerView: View, val headerId: Int) : androidx.recyclerview.widget.RecyclerView.ItemDecoration() {

//    private var headerView: View

    init {
//        headerView = recycler.inflate(R.layout.item_photo_header)
////        Log.w("Sirelon", "HEADER WIDTH = ${headerView.width} Recycler WIDTH = ${recycler.measuredWidth}")
        headerView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
//
//        headerView.setOnClickListener {   Log.w("Sirelon", "ON CLICK OCCURED") }
    }

    override fun getItemOffsets(outRect: Rect?, view: View, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State?) {
        super.getItemOffsets(outRect, view, parent, state)

        val itemPosition = parent.getChildAdapterPosition(view)
        if (itemPosition == androidx.recyclerview.widget.RecyclerView.NO_POSITION)
            return

//        if (headerId == parent.adapter.getItemViewType(itemPosition))
//            outRect?.top = 80

        if (itemPosition == 0 || itemPosition == 1)
            outRect?.top = headerView.measuredHeight
    }

    override fun onDrawOver(c: Canvas, parent: androidx.recyclerview.widget.RecyclerView, state: androidx.recyclerview.widget.RecyclerView.State?) {
        super.onDrawOver(c, parent, state)

        val childCount = parent.childCount
        if (childCount <= 0 || parent.adapter?.itemCount!! <= 0) {
            return
        }

        headerView.layout(parent.left, 0, parent.right, headerView.measuredHeight)

        for (i in 0..childCount) {
            val view = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(view)
            if (position == androidx.recyclerview.widget.RecyclerView.NO_POSITION)
                continue

            if (parent.adapter.getItemViewType(position) == headerId){

//            if (position == 0) {

                val transX = parent.left
                val transY = Math.max(view.top - headerView.height, 0)

                c.save()
                c.translate(transX.toFloat(), transY.toFloat())
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