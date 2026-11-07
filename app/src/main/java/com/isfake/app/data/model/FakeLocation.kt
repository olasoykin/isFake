package com.isfake.app.data.model

import java.util.UUID

data class FakeLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 10.0,
    val accuracy: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun latFormatted(): String = "%.6f".format(latitude)
    fun lngFormatted(): String = "%.6f".format(longitude)
    fun displayCoords(): String = "${latFormatted()}, ${lngFormatted()}"
}
