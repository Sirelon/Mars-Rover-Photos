package com.sirelon.marsroverphotos.widget

import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoViewAttacher
import com.sirelon.marsroverphotos.R
import com.sirelon.marsroverphotos.adapter.diffutils.ItemDiffCallback
import com.sirelon.marsroverphotos.extensions.inflate
import com.sirelon.marsroverphotos.storage.MarsImage

/**
 * @author romanishin
 * @since 16.11.16 on 18:11
 */
class ImagesPagerAdapter(
    private val scaleCallback: (() -> Unit),
    private val favoriteCallback: ((photo: MarsImage) -> Unit)
) : ListAdapter<MarsImage, ImagesPagerAdapter.ImageViewHolder>(ItemDiffCallback()) {

    private val colorsArr = listOf<Int>(
        Color.CYAN,
        Color.BLUE,
        Color.BLACK,
        Color.GRAY,
        Color.MAGENTA,
        Color.DKGRAY,
        Color.YELLOW,
        Color.RED,
        Color.GREEN
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ImageViewHolder(parent)

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val item = getItem(position)

        showImage(holder.imageView, item)
        holder.iconFav.isSelected = item.favorite
        holder.iconFav.setOnClickListener {
            favoriteCallback(item)
        }
    }

    private fun showImage(imageView: ImageView, marsPhoto: MarsImage) {
        val photoViewAttacher = PhotoViewAttacher(imageView)

        photoViewAttacher.setOnScaleChangeListener { _, _, _ ->
            scaleCallback.invoke()
        }

        val drawable = CircularProgressDrawable(imageView.context)
        drawable.strokeWidth = 15f
        drawable.setStyle(CircularProgressDrawable.LARGE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable.applyTheme(imageView.resources.newTheme())
        }
        drawable.setColorSchemeColors(*colorsArr.shuffled().toIntArray())
        drawable.start()

        Glide.with(imageView)
            .load(marsPhoto.imageUrl)
            .placeholder(drawable)
            .into(imageView)
    }

    class ImageViewHolder(parent: ViewGroup) :
        RecyclerView.ViewHolder(parent.inflate(R.layout.view_image)) {

        val imageView: ImageView = itemView.findViewById(R.id.fullscreenImage)
        val iconFav: View = itemView.findViewById(R.id.favBtn)
    }
}