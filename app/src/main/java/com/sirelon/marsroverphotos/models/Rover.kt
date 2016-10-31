package com.sirelon.marsroverphotos.models

/**
 * @author romanishin
 * @since 31.10.16 on 15:14
 */
data class Rover(
        var id: Long,
        var name: String,
        var landingDate: String,
        var launchDate: String,
        var staus: String,
        var maxSol: Long,
        var maxDate: String,
        var totalPhotos: Int) {

}