package com.sirelon.marsroverphotos.models

import android.os.Parcel
import android.os.Parcelable

/**
 * @author romanishin
 * @since 31.10.16 on 19:53
 */
data class PhotosQueryRequest(
        var roverName: String,
        var sol: Long,
        var camera: String?
) : Parcelable {
    companion object {
        @JvmField val CREATOR: Parcelable.Creator<PhotosQueryRequest> = object : Parcelable.Creator<PhotosQueryRequest> {
            override fun createFromParcel(source: Parcel): PhotosQueryRequest = PhotosQueryRequest(source)
            override fun newArray(size: Int): Array<PhotosQueryRequest?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(source.readString(), source.readLong(), source.readString())

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        dest?.writeString(roverName)
        dest?.writeLong(sol)
        dest?.writeString(camera)
    }
}