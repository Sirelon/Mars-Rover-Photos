package com.sirelon.marsroverphotos.utils

import kotlin.random.Random

/**
 * Random generator utilities for KMP.
 * Created on 12/21/17 12:12 for genetator-android.
 * Migrated to KMP using kotlin.random.Random
 */
object RandomGenerator {
    /**
     * Generate a random UUID string.
     * @return UUID in format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx
     */
    fun randomUUID(): String {
        val hexChars = "0123456789abcdef"
        val uuid = CharArray(36)

        for (i in uuid.indices) {
            when (i) {
                8, 13, 18, 23 -> uuid[i] = '-'
                14 -> uuid[i] = '4' // UUID version 4
                19 -> uuid[i] = hexChars[Random.nextInt(0, 4) + 8] // 8, 9, a, or b
                else -> uuid[i] = hexChars[Random.nextInt(0, 16)]
            }
        }

        return String(uuid)
    }
}

fun randomDouble(): Double {
    val leftLimit = 0.1
    val rightLimit = 0.0000001
    return leftLimit + Random.nextDouble() * (rightLimit - leftLimit)
}

fun randomLong() = Random.nextLong()

fun randomInt(from: Int, to: Int): Int {
    return Random.nextInt(from, to)
}

fun randomRole(): String {
    val random = randomInt(1, 10)

    return when (random) {
        1 -> "Android developer"
        2 -> "iOS Developer"
        3 -> "UX Designer"
        4 -> "Product owner"
        5 -> "Product manager"
        else -> "Random position"
    }
}

fun randomImageUrl() = "https://picsum.photos/240/360?random=" + randomInt(0, 10000)
