package com.stem.heartsync.data.model

data class WeatherData(
    val temperature: Double,
    val apparentTemperature: Double,
    val humidity: Int,
    val windSpeed: Double,
    val weatherCode: Int,
    val conditionDescription: String,
    val conditionEmoji: String,
    val cityName: String,
    val isDay: Boolean = true,
    val cardiacHealthTip: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isCached: Boolean = false,
    val isLocationOff: Boolean = false
)

enum class LocationStatus {
    AVAILABLE,
    PERMISSION_DENIED,
    GPS_OFF
}

data class CityPreset(
    val name: String,
    val latitude: Double,
    val longitude: Double
)
