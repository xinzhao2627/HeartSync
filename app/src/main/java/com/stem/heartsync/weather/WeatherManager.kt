package com.stem.heartsync.weather

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.stem.heartsync.data.model.CityPreset
import com.stem.heartsync.data.model.LocationStatus
import com.stem.heartsync.data.model.WeatherData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class WeatherManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val prefs: SharedPreferences = context.getSharedPreferences("heartsync_weather_prefs", Context.MODE_PRIVATE)

    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private val _locationStatus = MutableStateFlow(LocationStatus.GPS_OFF)
    val locationStatus: StateFlow<LocationStatus> = _locationStatus.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedCity = MutableStateFlow<CityPreset?>(null)
    val selectedCity: StateFlow<CityPreset?> = _selectedCity.asStateFlow()

    val cityPresets = listOf(
        CityPreset("Manila", 14.5995, 120.9842),
        CityPreset("Quezon City", 14.6760, 121.0437),
        CityPreset("Baguio City", 16.4023, 120.5960),
        CityPreset("Cebu City", 10.3157, 123.8854),
        CityPreset("Davao City", 7.1907, 125.4553),
        CityPreset("Iloilo City", 10.7202, 122.5621),
        CityPreset("Cagayan de Oro", 8.4542, 124.6319)
    )

    init {
        loadCachedWeather()
        checkLocationStatus()
        refreshWeather()
    }

    fun checkLocationStatus(): LocationStatus {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            _locationStatus.value = LocationStatus.PERMISSION_DENIED
            return LocationStatus.PERMISSION_DENIED
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val isGpsEnabled = lm?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = lm?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        val status = if (isGpsEnabled || isNetworkEnabled) {
            LocationStatus.AVAILABLE
        } else {
            LocationStatus.GPS_OFF
        }

        _locationStatus.value = status
        return status
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun selectPresetCity(preset: CityPreset) {
        _selectedCity.value = preset
        prefs.edit()
            .putString("selected_city_name", preset.name)
            .putFloat("selected_city_lat", preset.latitude.toFloat())
            .putFloat("selected_city_lon", preset.longitude.toFloat())
            .apply()
        refreshWeather(forceNetwork = true)
    }

    fun clearManualCity() {
        _selectedCity.value = null
        prefs.edit()
            .remove("selected_city_name")
            .remove("selected_city_lat")
            .remove("selected_city_lon")
            .apply()
        checkLocationStatus()
        refreshWeather(forceNetwork = true)
    }

    fun refreshWeather(forceNetwork: Boolean = false) {
        scope.launch {
            _isRefreshing.value = true
            try {
                val locStatus = checkLocationStatus()
                val manualCity = _selectedCity.value ?: loadSavedManualCity()

                var targetLat = 14.5995
                var targetLon = 120.9842
                var resolvedCityName = "Manila, PH"
                val isLocationOff = locStatus != LocationStatus.AVAILABLE && manualCity == null

                if (manualCity != null) {
                    targetLat = manualCity.latitude
                    targetLon = manualCity.longitude
                    resolvedCityName = manualCity.name
                } else if (locStatus == LocationStatus.AVAILABLE) {
                    val currentLocation = getDeviceLocation()
                    if (currentLocation != null) {
                        targetLat = currentLocation.latitude
                        targetLon = currentLocation.longitude
                        resolvedCityName = resolveCityName(targetLat, targetLon)
                    }
                }

                if (!isOnline()) {
                    // Offline fallback: load cached weather if exists
                    val cached = loadCachedWeather()
                    if (cached != null) {
                        _weather.value = cached.copy(isCached = true, isLocationOff = isLocationOff)
                    } else {
                        // Create basic offline placeholder with default coordinates
                        val placeholder = WeatherData(
                            temperature = 29.0,
                            apparentTemperature = 32.0,
                            humidity = 75,
                            windSpeed = 10.0,
                            weatherCode = 1,
                            conditionDescription = "Partly Cloudy (Offline Mode)",
                            conditionEmoji = "⛅",
                            cityName = resolvedCityName,
                            isDay = true,
                            cardiacHealthTip = "Connect to internet when available to fetch live meteorological telemetry.",
                            timestamp = System.currentTimeMillis(),
                            isCached = true,
                            isLocationOff = isLocationOff
                        )
                        _weather.value = placeholder
                    }
                    return@launch
                }

                // Online: fetch from Open-Meteo
                val fetched = fetchFromOpenMeteo(targetLat, targetLon, resolvedCityName, isLocationOff)
                if (fetched != null) {
                    saveWeatherCache(fetched)
                    _weather.value = fetched
                    evaluateWeatherAndNotify(fetched)
                } else {
                    // Fallback to cache if network request failed
                    val cached = loadCachedWeather()
                    if (cached != null) {
                        _weather.value = cached.copy(isCached = true, isLocationOff = isLocationOff)
                    }
                }
            } catch (e: Exception) {
                val cached = loadCachedWeather()
                if (cached != null) {
                    _weather.value = cached.copy(isCached = true)
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getDeviceLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        var bestLocation: Location? = null

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) return null

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val l = lm.getLastKnownLocation(provider)
                    if (l != null && (bestLocation == null || l.accuracy < bestLocation.accuracy)) {
                        bestLocation = l
                    }
                }
            } catch (e: SecurityException) {
                // Ignore security exceptions
            }
        }
        return bestLocation
    }

    @Suppress("DEPRECATION")
    private fun resolveCityName(lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: String.format(Locale.US, "Lat %.2f, Lon %.2f", lat, lon)
        } catch (e: Exception) {
            String.format(Locale.US, "Lat %.2f, Lon %.2f", lat, lon)
        }
    }

    private suspend fun fetchFromOpenMeteo(
        lat: Double,
        lon: Double,
        cityName: String,
        isLocationOff: Boolean
    ): WeatherData? = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val urlString = String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m",
                lat,
                lon
            )
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "HeartSync-Mobile/1.0")

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(response)
                val current = root.getJSONObject("current")

                val temp = current.getDouble("temperature_2m")
                val apparentTemp = current.getDouble("apparent_temperature")
                val humidity = current.getInt("relative_humidity_2m")
                val windSpeed = current.getDouble("wind_speed_10m")
                val isDay = current.getInt("is_day") == 1
                val code = current.getInt("weather_code")

                val (conditionText, emoji) = mapWeatherCodeToDescription(code, isDay)
                val cardiacTip = calculateCardiacHealthTip(apparentTemp, humidity, code)

                return@withContext WeatherData(
                    temperature = temp,
                    apparentTemperature = apparentTemp,
                    humidity = humidity,
                    windSpeed = windSpeed,
                    weatherCode = code,
                    conditionDescription = conditionText,
                    conditionEmoji = emoji,
                    cityName = cityName,
                    isDay = isDay,
                    cardiacHealthTip = cardiacTip,
                    timestamp = System.currentTimeMillis(),
                    isCached = false,
                    isLocationOff = isLocationOff
                )
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun mapWeatherCodeToDescription(code: Int, isDay: Boolean): Pair<String, String> {
        return when (code) {
            0 -> if (isDay) "Clear Sky" to "☀️" else "Clear Night" to "🌙"
            1 -> if (isDay) "Mainly Clear" to "🌤️" else "Mostly Clear" to "🌤️"
            2 -> "Partly Cloudy" to "⛅"
            3 -> "Overcast" to "☁️"
            45, 48 -> "Foggy Atmosphere" to "🌫️"
            51, 53, 55 -> "Light Drizzle" to "🌦️"
            56, 57 -> "Freezing Drizzle" to "🌧️"
            61, 63, 65 -> "Rainy Weather" to "🌧️"
            66, 67 -> "Freezing Rain" to "🌧️"
            71, 73, 75, 77 -> "Light Snow" to "❄️"
            80, 81, 82 -> "Passing Rain Showers" to "🌧️"
            85, 86 -> "Snow Showers" to "🌨️"
            95 -> "Thunderstorm" to "⛈️"
            96, 99 -> "Severe Thunderstorm" to "⛈️"
            else -> "Mild Conditions" to "🌤️"
        }
    }

    private fun calculateCardiacHealthTip(apparentTemp: Double, humidity: Int, code: Int): String {
        return when {
            apparentTemp >= 35.0 ->
                "High Heat Alert: Extreme heat increases heart workload. Remain in shaded or cool spaces, rest frequently, and drink water."
            apparentTemp >= 31.0 ->
                "Warm Index: Sip water consistently throughout your school day. Avoid strenuous sprint or midday activities."
            apparentTemp <= 19.0 ->
                "Cool Air Alert: Lower temperatures can induce vascular constriction. Wear warm layers to keep blood pressure steady."
            humidity >= 80 ->
                "High Humidity: Sweating is less efficient in humid air, adding heart strain. Take gentle rest pauses between classes."
            code in listOf(95, 96, 99) ->
                "Thunderstorm Alert: Sudden barometric changes may cause fatigue. Stay indoors, take meds on time, and relax."
            code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) ->
                "Rainy Climate: Stay dry and warm; sudden cold chills trigger blood vessel constriction."
            else ->
                "Balanced Weather: Favorable cardiovascular weather today. Good conditions for peaceful routines and steady breathing."
        }
    }

    private fun saveWeatherCache(data: WeatherData) {
        prefs.edit()
            .putFloat("cached_temp", data.temperature.toFloat())
            .putFloat("cached_apparent_temp", data.apparentTemperature.toFloat())
            .putInt("cached_humidity", data.humidity)
            .putFloat("cached_wind_speed", data.windSpeed.toFloat())
            .putInt("cached_weather_code", data.weatherCode)
            .putString("cached_condition_text", data.conditionDescription)
            .putString("cached_condition_emoji", data.conditionEmoji)
            .putString("cached_city_name", data.cityName)
            .putBoolean("cached_is_day", data.isDay)
            .putString("cached_cardiac_tip", data.cardiacHealthTip)
            .putLong("cached_timestamp", data.timestamp)
            .apply()
    }

    private fun loadCachedWeather(): WeatherData? {
        if (!prefs.contains("cached_temp")) return null
        val temp = prefs.getFloat("cached_temp", 28f).toDouble()
        val apparent = prefs.getFloat("cached_apparent_temp", 31f).toDouble()
        val humidity = prefs.getInt("cached_humidity", 70)
        val wind = prefs.getFloat("cached_wind_speed", 8f).toDouble()
        val code = prefs.getInt("cached_weather_code", 1)
        val condition = prefs.getString("cached_condition_text", "Partly Cloudy") ?: "Partly Cloudy"
        val emoji = prefs.getString("cached_condition_emoji", "⛅") ?: "⛅"
        val city = prefs.getString("cached_city_name", "Local Area") ?: "Local Area"
        val isDay = prefs.getBoolean("cached_is_day", true)
        val tip = prefs.getString("cached_cardiac_tip", "Stay well hydrated and adhere to your daily cardiac care plan.")
            ?: "Stay well hydrated and adhere to your daily cardiac care plan."
        val ts = prefs.getLong("cached_timestamp", System.currentTimeMillis())

        val isLocOff = checkLocationStatus() != LocationStatus.AVAILABLE && _selectedCity.value == null

        val data = WeatherData(
            temperature = temp,
            apparentTemperature = apparent,
            humidity = humidity,
            windSpeed = wind,
            weatherCode = code,
            conditionDescription = condition,
            conditionEmoji = emoji,
            cityName = city,
            isDay = isDay,
            cardiacHealthTip = tip,
            timestamp = ts,
            isCached = true,
            isLocationOff = isLocOff
        )
        _weather.value = data
        return data
    }

    fun evaluateWeatherAndNotify(weatherData: WeatherData, force: Boolean = false) {
        val isHighHeat = weatherData.apparentTemperature >= 35.0 || weatherData.temperature >= 35.0
        val isColdAlert = weatherData.apparentTemperature <= 19.0 || weatherData.temperature <= 19.0

        if (isHighHeat || isColdAlert) {
            val dbHelper = com.stem.heartsync.data.local.HeartSyncDbHelper.getInstance(context)
            val prefs = dbHelper.getUserPreferences()
            val notifManager = com.stem.heartsync.notification.HeartSyncNotificationManager.getInstance(context)
            notifManager.showWeatherAlert(
                temperature = weatherData.temperature,
                apparentTemperature = weatherData.apparentTemperature,
                conditionText = weatherData.conditionDescription,
                cardiacTip = weatherData.cardiacHealthTip,
                prefs = prefs,
                isHighHeat = isHighHeat,
                force = force
            )
        }
    }

    fun checkWeatherForAlerts() {
        refreshWeather(forceNetwork = true)
    }

    fun triggerTestWeatherAlert(isHighHeat: Boolean) {
        val dbHelper = com.stem.heartsync.data.local.HeartSyncDbHelper.getInstance(context)
        val prefs = dbHelper.getUserPreferences()
        val notifManager = com.stem.heartsync.notification.HeartSyncNotificationManager.getInstance(context)
        val testTemp = if (isHighHeat) 36.0 else 17.0
        val testApparent = if (isHighHeat) 40.0 else 16.0
        val testDesc = if (isHighHeat) "Extreme Solar Heat Index" else "Cool Ambient Breeze"
        val testTip = if (isHighHeat) {
            "High Heat Alert: Extreme heat increases heart workload. Remain in shaded or cool spaces, rest frequently, and drink water."
        } else {
            "Cool Air Alert: Lower temperatures can induce vascular constriction. Wear warm layers to keep blood pressure steady."
        }
        notifManager.showWeatherAlert(
            temperature = testTemp,
            apparentTemperature = testApparent,
            conditionText = testDesc,
            cardiacTip = testTip,
            prefs = prefs,
            isHighHeat = isHighHeat,
            force = true
        )
    }

    private fun loadSavedManualCity(): CityPreset? {
        val name = prefs.getString("selected_city_name", null) ?: return null
        val lat = prefs.getFloat("selected_city_lat", 14.5995f).toDouble()
        val lon = prefs.getFloat("selected_city_lon", 120.9842f).toDouble()
        val preset = CityPreset(name, lat, lon)
        _selectedCity.value = preset
        return preset
    }

    companion object {
        @Volatile
        private var instance: WeatherManager? = null

        fun getInstance(context: Context): WeatherManager {
            return instance ?: synchronized(this) {
                instance ?: WeatherManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
