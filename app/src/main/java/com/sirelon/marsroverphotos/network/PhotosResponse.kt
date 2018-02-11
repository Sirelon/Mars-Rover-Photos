package com.sirelon.marsroverphotos.network

import android.support.annotation.Keep
import com.sirelon.marsroverphotos.models.MarsPhoto

/**
 * @author romanishin
 * @since 31.10.16 on 15:49
 */
@Keep
data class PhotosResponse ( val photos: List<MarsPhoto>)