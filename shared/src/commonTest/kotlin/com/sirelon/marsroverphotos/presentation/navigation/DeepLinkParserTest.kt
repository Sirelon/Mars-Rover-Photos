package com.sirelon.marsroverphotos.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkParserTest {

    @Test
    fun customScheme_rover_parsesId() {
        assertEquals(DeepLink.Rover(5), parseDeepLink("marsrover://rover/5"))
    }

    @Test
    fun customScheme_photo_parsesId() {
        assertEquals(DeepLink.Photo(12345), parseDeepLink("marsrover://photo/12345"))
    }

    @Test
    fun webLink_rover_parsesId() {
        assertEquals(DeepLink.Rover(3), parseDeepLink("https://marsroverphotos.app/rover/3"))
    }

    @Test
    fun webLink_photo_parsesId() {
        assertEquals(DeepLink.Photo(99), parseDeepLink("https://marsroverphotos.app/photo/99"))
    }

    @Test
    fun hostCasing_isIgnored() {
        assertEquals(DeepLink.Rover(7), parseDeepLink("marsrover://ROVER/7"))
        assertEquals(DeepLink.Rover(7), parseDeepLink("https://MarsRoverPhotos.app/Rover/7"))
    }

    @Test
    fun trailingSlash_isTolerated() {
        assertEquals(DeepLink.Rover(6), parseDeepLink("marsrover://rover/6/"))
    }

    @Test
    fun queryAndFragment_areStripped() {
        assertEquals(DeepLink.Rover(5), parseDeepLink("marsrover://rover/5?utm_source=push"))
        assertEquals(DeepLink.Photo(8), parseDeepLink("https://marsroverphotos.app/photo/8#top"))
    }

    @Test
    fun whatsNew_withVersion_parsesVersion() {
        assertEquals(DeepLink.WhatsNew("4.2.0"), parseDeepLink("marsrover://whatsnew/4.2.0"))
    }

    @Test
    fun whatsNew_withoutVersion_parsesToNullVersion() {
        assertEquals(DeepLink.WhatsNew(null), parseDeepLink("marsrover://whatsnew"))
        assertEquals(DeepLink.WhatsNew(null), parseDeepLink("marsrover://whatsnew/"))
    }

    @Test
    fun nonNumericId_returnsNull() {
        assertNull(parseDeepLink("marsrover://rover/curiosity"))
        assertNull(parseDeepLink("https://marsroverphotos.app/photo/abc"))
    }

    @Test
    fun missingId_returnsNull() {
        assertNull(parseDeepLink("marsrover://rover"))
        assertNull(parseDeepLink("marsrover://rover/"))
        assertNull(parseDeepLink("https://marsroverphotos.app/rover"))
    }

    @Test
    fun unknownKind_returnsNull() {
        assertNull(parseDeepLink("marsrover://mission/5"))
        assertNull(parseDeepLink("https://marsroverphotos.app/mission/5"))
    }

    @Test
    fun unrelatedHost_returnsNull() {
        assertNull(parseDeepLink("https://example.com/rover/5"))
    }

    @Test
    fun malformedInput_returnsNull() {
        assertNull(parseDeepLink(""))
        assertNull(parseDeepLink("not a uri"))
        assertNull(parseDeepLink("marsrover://"))
    }

    @Test
    fun idOutOfLongRange_returnsNull() {
        assertNull(parseDeepLink("marsrover://photo/99999999999999999999"))
    }
}
