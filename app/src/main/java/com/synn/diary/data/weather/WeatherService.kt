package com.synn.diary.data.weather

import android.Manifest
import android.annotation.SuppressLint
import android.util.Log
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

data class WeatherSnapshot(
    val weatherText: String?,
    val locationText: String?,
    val latitude: Double,
    val longitude: Double,
)

/**
 * 抓一次"写这条时的环境"：定位 → 取天气 → 反查城市名。
 *
 * 三个刻意的选择：
 * 1. 用平台 LocationManager，不用 play-services-location —— 国内 ROM 常常没有 GMS，装了也白搭。
 * 2. 天气走 open-meteo，不需要 API key、不注册账号，请求里只有经纬度，没有设备标识。
 * 3. 任何一步失败都返回 null，绝不阻塞写作，也不编造天气。
 */
class WeatherService(private val context: Context) {

    suspend fun capture(): WeatherSnapshot? = withContext(Dispatchers.IO) {
        val location = currentLocation()
        Log.d(TAG, "capture: permission=${hasLocationPermission()} location=$location")
        if (location == null) return@withContext null
        val weather = runCatching { fetchWeather(location.latitude, location.longitude) }.getOrNull()
        // 反查城市名依赖系统 Geocoder（国内多数 ROM 走 Google 服务，基本查不到），
        // 查不到就退回经纬度：至少能证明"当时在哪"，不编造城市名。
        val place = withTimeoutOrNull(GEOCODE_TIMEOUT_MS) {
            runCatching { reverseGeocode(location.latitude, location.longitude) }.getOrNull()
        } ?: formatCoordinates(location.latitude, location.longitude)
        Log.d(TAG, "capture: weather=$weather place=$place")
        WeatherSnapshot(
            weatherText = weather,
            locationText = place,
            latitude = location.latitude,
            longitude = location.longitude,
        )
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null

        // 快路径：最近 10 分钟内有过定位，直接用，不必等新的 fix。
        // 这段是"打开就写"的关键——写日记的触发器不能被定位拖十几秒。
        lastKnown(manager)?.let { cached ->
            if (System.currentTimeMillis() - cached.time < FRESH_FIX_MS) return cached
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 两个 provider 并行等：两个都起，先 await 网络、再 await GPS。
            // GPS 通常更快（模拟器/户外），但因为并行，总耗时就是单个超时，不是两倍。
            val fresh = coroutineScope {
                val networkFx = async {
                    withTimeoutOrNull(FIX_TIMEOUT_MS) {
                        runCatching { currentFix(manager, LocationManager.NETWORK_PROVIDER) }.getOrNull()
                    }
                }
                val gpsFx = async {
                    withTimeoutOrNull(FIX_TIMEOUT_MS) {
                        runCatching { currentFix(manager, LocationManager.GPS_PROVIDER) }.getOrNull()
                    }
                }
                gpsFx.await() ?: networkFx.await()
            }
            if (fresh != null) return fresh
        }
        return lastKnown(manager)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("MissingPermission")
    private suspend fun currentFix(manager: LocationManager, provider: String): Location? {
        if (!manager.isProviderEnabled(provider)) return null
        return suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnown(manager: LocationManager): Location? {
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        return providers
            .asSequence()
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
    }

    private fun reverseGeocode(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.CHINA)
        @Suppress("DEPRECATION")
        val results = geocoder.getFromLocation(latitude, longitude, 1).orEmpty()
        val address = results.firstOrNull() ?: return null
        // 展示到"市"就够了；区县名在中文地名里经常和市名重复
        return listOfNotNull(address.adminArea, address.locality ?: address.subAdminArea)
            .distinct()
            .joinToString(" ")
            .takeIf { it.isNotBlank() }
    }

    private fun formatCoordinates(latitude: Double, longitude: Double): String {
        val lat = String.format(Locale.US, "%.2f°%s", abs(latitude), if (latitude >= 0) "N" else "S")
        val lon = String.format(Locale.US, "%.2f°%s", abs(longitude), if (longitude >= 0) "E" else "W")
        return "$lat $lon"
    }

    private fun fetchWeather(latitude: Double, longitude: Double): String? {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,weather_code&timezone=auto",
            latitude,
            longitude,
        )
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = NET_TIMEOUT_MS
            readTimeout = NET_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).optJSONObject("current") ?: return null
            val temperature = current.optDouble("temperature_2m", Double.NaN)
            val code = current.optInt("weather_code", -1)
            if (temperature.isNaN() || code < 0) return null
            "${describeWeather(code)} ${temperature.roundToInt()}°"
        } finally {
            connection.disconnect()
        }
    }

    /** WMO weather code → 中文短描述，只保留日历/卡片上放得下的说法。 */
    private fun describeWeather(code: Int): String = when (code) {
        0 -> "晴"
        1 -> "晴间多云"
        2 -> "多云"
        3 -> "阴"
        45, 48 -> "雾"
        51, 53, 55, 56, 57 -> "毛毛雨"
        61, 63 -> "小雨"
        65 -> "大雨"
        66, 67 -> "冻雨"
        71, 73 -> "小雪"
        75 -> "大雪"
        77 -> "米雪"
        80, 81 -> "阵雨"
        82 -> "强阵雨"
        85, 86 -> "阵雪"
        95 -> "雷阵雨"
        96, 99 -> "雷暴冰雹"
        else -> "天气未知"
    }

    private companion object {
        const val TAG = "WeatherService"
        const val FIX_TIMEOUT_MS = 3_000L
        const val FRESH_FIX_MS = 10 * 60 * 1000L
        const val GEOCODE_TIMEOUT_MS = 2_500L
        const val NET_TIMEOUT_MS = 4_000
    }
}
