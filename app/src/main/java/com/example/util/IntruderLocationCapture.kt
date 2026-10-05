package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.Locale

/** 침입 시점에 확보한 위치 정보. 기기 안에만 남는다. */
data class CaptureLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Int,
    val coordinatesText: String,
    val placeName: String? = null
)

/**
 * 침입 증거가 촬영되는 그 순간의 위치를 확보한다.
 *
 * - 권한이 없으면 아무것도 하지 않는다(조용히 실패).
 * - 네트워크/GPS 의 마지막 알려진 위치를 사용하므로 요청으로 시간을 소비하지 않는다.
 * - 서버로 전송하지 않으며, 앱 내부 저장소에만 남는다.
 */
object IntruderLocationCapture {

    private const val MAX_AGE_MS = 10 * 60 * 1000L // 10분 이상 지난 위치는 신뢰하지 않는다

    fun capture(context: Context): CaptureLocation? {
        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return null

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val now = System.currentTimeMillis()
        val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { now - it.time <= MAX_AGE_MS }
            .maxByOrNull { it.time }
            ?: return null

        val coordinates = "%.6f, %.6f (±%dm)".format(Locale.US, location.latitude, location.longitude, location.accuracy.toInt())
        return CaptureLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy.toInt(),
            coordinatesText = coordinates,
            placeName = resolvePlaceName(context, location)
        )
    }

    /** 역지오코딩은 실패해도 무방하므로 결과를 반환하지 않는다. */
    private fun resolvePlaceName(context: Context, location: Location): String? = runCatching {
        if (!Geocoder.isPresent()) return null
        @Suppress("DEPRECATION")
        val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
        addresses?.firstOrNull()?.let { address ->
            listOfNotNull(
                address.locality ?: address.subAdminArea,
                address.thoroughfare,
                address.featureName
            ).takeIf { it.isNotEmpty() }?.joinToString(" ")
        }
    }.getOrNull()
}