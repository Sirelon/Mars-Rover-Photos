package com.sirelon.marsroverphotos.utils

import java.util.Random

/**
 * Created on 12/21/17 12:12 for genetator-android.
 */
fun randomDouble(): Double {
    val leftLimit = 0.1
    val rightLimit = 0.0000001
    return leftLimit + Random().nextDouble() * (rightLimit - leftLimit)
}

fun randomLong() = Random().nextLong()

fun randomInt(from: Int, to: Int): Int {
    return Random().nextInt(to - from) + from
}

fun randomRole(): String {
    val random = randomInt(1, 10)

    return when (random) {
        1    -> "Android developer"
        2    -> "iOs Developer"
        3    -> "UX Designer"
        4    -> "Product owner"
        5    -> "Product manager"
        else -> "Random position"
    }
}

fun randomImageUrl() = "https://picsum.photos/240/360?random=" + randomInt(0, 10000)