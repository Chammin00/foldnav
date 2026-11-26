package com.foldnav.app.nav

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PlaceResult(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

data class RouteResult(
    val destLabel: String,
    val destLat: Double,
    val destLon: Double,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val firstInstruction: String,
    val geoJsonFeatureCollection: String
)

object RoutingClient {
    private const val TIMEOUT_MS = 9000

    suspend fun searchPlaces(query: String): List<PlaceResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()
        val encoded = URLEncoder.encode(trimmed, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?format=json&limit=5&q=$encoded")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "FoldNav-ZFold6/1.0")
        }
        try {
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            val list = ArrayList<PlaceResult>(arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("display_name").ifBlank { trimmed }
                val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                list.add(PlaceResult(name, lat, lon))
            }
            list
        } catch (_: Throwable) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    suspend fun fetchDrivingRoute(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        destLabel: String
    ): RouteResult? = withContext(Dispatchers.IO) {
        val url = URL(
            "https://router.project-osrm.org/route/v1/driving/" +
                "$fromLon,$fromLat;$toLon,$toLat?overview=full&geometries=geojson&steps=true"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "FoldNav-ZFold6/1.0")
        }
        try {
            if (conn.responseCode !in 200..299) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(body)
            val routes = root.optJSONArray("routes") ?: return@withContext null
            if (routes.length() == 0) return@withContext null

            val route = routes.getJSONObject(0)
            val distance = route.optDouble("distance", 0.0)
            val duration = route.optDouble("duration", 0.0)
            val geometry = route.optJSONObject("geometry") ?: return@withContext null

            var firstCue = "Proceed toward $destLabel"
            val legs = route.optJSONArray("legs")
            if (legs != null && legs.length() > 0) {
                val steps = legs.getJSONObject(0).optJSONArray("steps")
                if (steps != null && steps.length() > 0) {
                    val step0 = steps.getJSONObject(0)
                    val road = step0.optString("name", "")
                    val man = step0.optJSONObject("maneuver")
                    val type = man?.optString("type", "head") ?: "head"
                    val mod = man?.optString("modifier", "") ?: ""
                    val cue = listOf(type, mod, if (road.isNotBlank()) "on $road" else "")
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .replaceFirstChar { it.uppercase() }
                    if (cue.isNotBlank()) firstCue = cue
                }
            }

            val feature = JSONObject().apply {
                put("type", "Feature")
                put("properties", JSONObject())
                put("geometry", geometry)
            }
            val collection = JSONObject().apply {
                put("type", "FeatureCollection")
                put("features", JSONArray(listOf(feature)))
            }

            RouteResult(
                destLabel = destLabel,
                destLat = toLat,
                destLon = toLon,
                distanceMeters = distance,
                durationSeconds = duration,
                firstInstruction = firstCue,
                geoJsonFeatureCollection = collection.toString()
            )
        } catch (_: Throwable) {
            null
        } finally {
            conn.disconnect()
        }
    }
}

object GoogleMapsLauncher {
    fun launchTurnByTurn(
        context: Context,
        lat: Double,
        lon: Double,
        label: String,
        splitScreenAdjacent: Boolean
    ): Boolean {
        val navIntent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lon&mode=d")).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (splitScreenAdjacent) {
                addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            }
        }
        return try {
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
                true
            } else {
                val encodedLabel = Uri.encode(label)
                val geoIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("geo:$lat,$lon?q=$lat,$lon($encodedLabel)")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (splitScreenAdjacent) {
                        addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
                    }
                }
                context.startActivity(geoIntent)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }
}
