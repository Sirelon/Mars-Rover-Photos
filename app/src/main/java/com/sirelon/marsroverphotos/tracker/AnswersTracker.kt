package com.sirelon.marsroverphotos.tracker

import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.ContentViewEvent
import com.crashlytics.android.answers.CustomEvent
import com.crashlytics.android.answers.ShareEvent
import com.sirelon.marsroverphotos.models.MarsPhoto

/**
 * Created on 19/04/2017 15:33.
 */
class AnswersTracker : ITracker {

    companion object {
        private val TYPE_MARS_PHOTO: String = "MARS_PHOTO"
        private val ATTR_MARS_PHOTO_URL: String = "photoUrl"
        private val ATTR_MARS_PHOTO_ID: String = "photoId"
        private val ATTR_MARS_PHOTO_NAME: String = "photoName"
        private val EVENT_MARS_PHOTO_SCALE: String = "MarsPhotoScale"
        private val EVENT_MARS_PHOTO_SAVE: String = "MarsPhotoSave"
    }

    override fun trackSeen(photo: MarsPhoto) {
        Answers.getInstance().logContentView(ContentViewEvent()
                .putContentId(photo.id.toString())
                .putContentName(photo.name)
                .putContentType(TYPE_MARS_PHOTO)
                .putCustomAttribute(ATTR_MARS_PHOTO_URL, photo.imageUrl))
    }

    override fun trackScale(photo: MarsPhoto) {
        Answers.getInstance().logCustom(CustomEvent(EVENT_MARS_PHOTO_SCALE)
                .putCustomAttribute(ATTR_MARS_PHOTO_ID, photo.id)
                .putCustomAttribute(ATTR_MARS_PHOTO_NAME, photo.name)
                .putCustomAttribute(ATTR_MARS_PHOTO_URL, photo.imageUrl))
    }

    override fun trackShare(photo: MarsPhoto, packageName: String) {
        Answers.getInstance().logShare(ShareEvent()
                .putMethod(packageName)
                .putContentId(photo.id.toString())
                .putContentName(photo.name)
                .putContentType(TYPE_MARS_PHOTO)
                .putCustomAttribute(ATTR_MARS_PHOTO_URL, photo.imageUrl))
    }

    override fun trackSave(photo: MarsPhoto) {
        Answers.getInstance().logCustom(CustomEvent(EVENT_MARS_PHOTO_SAVE)
                .putCustomAttribute(ATTR_MARS_PHOTO_ID, photo.id)
                .putCustomAttribute(ATTR_MARS_PHOTO_NAME, photo.name)
                .putCustomAttribute(ATTR_MARS_PHOTO_URL, photo.imageUrl))
    }
}