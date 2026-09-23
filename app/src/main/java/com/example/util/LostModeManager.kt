package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject

data class LostModeEvent(val timestamp: Long, val location: String?, val photoPath: String?)

/** Local-only Lost Mode state. It neither tracks in the background nor transmits data. */
object LostModeManager {
    private const val PREFS = "lost_mode"
    private const val ACTIVE = "active"
    private const val EVENTS = "events"
    private const val LAST_CAPTURE = "last_capture"
    private const val CAPTURE_INTERVAL_MS = 5 * 60 * 1000L

    fun isActive(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ACTIVE, false)
    fun setActive(context: Context, active: Boolean) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ACTIVE, active).apply()

    fun bestKnownLocation(context: Context): String? {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!allowed) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull(Location::getTime) ?: return null
        return "%.6f, %.6f (±%dm)".format(java.util.Locale.US, location.latitude, location.longitude, location.accuracy.toInt())
    }

    fun record(context: Context, location: String?, photoPath: String?) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val items = JSONArray(prefs.getString(EVENTS, "[]"))
        items.put(0, JSONObject().put("time", System.currentTimeMillis()).put("location", location).put("photo", photoPath))
        while (items.length() > 20) items.remove(items.length() - 1)
        prefs.edit().putString(EVENTS, items.toString()).putLong(LAST_CAPTURE, System.currentTimeMillis()).apply()
    }

    fun shouldCaptureOnOpen(context: Context): Boolean = isActive(context) &&
        System.currentTimeMillis() - context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(LAST_CAPTURE, 0L) >= CAPTURE_INTERVAL_MS

    fun events(context: Context): List<LostModeEvent> = runCatching {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(EVENTS, "[]"))
        List(array.length()) { index -> array.getJSONObject(index) }.map { item ->
            LostModeEvent(item.optLong("time"), item.optString("location").takeIf { it.isNotBlank() && it != "null" }, item.optString("photo").takeIf { it.isNotBlank() && it != "null" })
        }
    }.getOrDefault(emptyList())

    fun mapUri(location: String?): android.net.Uri? {
        val match = Regex("(-?\\d+\\.\\d+),\\s*(-?\\d+\\.\\d+)").find(location ?: "") ?: return null
        return android.net.Uri.parse("geo:${match.groupValues[1]},${match.groupValues[2]}?q=${match.groupValues[1]},${match.groupValues[2]}")
    }
}
