package com.example.core.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.core.content.ContextCompat

data class ManualLocationInput(
    val district: String = "",
    val thana: String = "",
    val city: String = "",
    val area: String = ""
) {
    fun toFormattedLocation(): String = listOf(area, city, thana, district)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString(", ")
}

object LocationSearchHelper {

    fun buildMapsQueryText(
        baseQuery: String,
        useCurrentLocation: Boolean,
        manualLocation: ManualLocationInput
    ): String {
        val cleanBase = baseQuery.trim()
        val manualText = manualLocation.toFormattedLocation()
        return when {
            !useCurrentLocation && manualText.isNotBlank() -> "$cleanBase $manualText".trim()
            useCurrentLocation && manualText.isNotBlank() -> "$cleanBase $manualText".trim()
            useCurrentLocation -> "$cleanBase near me".trim()
            else -> cleanBase
        }
    }

    fun buildMapsWebUrl(queryText: String): String =
        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(queryText.trim())}"

    fun buildWebSearchUrl(queryText: String): String =
        "https://www.google.com/search?q=${Uri.encode(queryText.trim())}"

    fun openGoogleMapsSearch(
        context: Context,
        baseQuery: String,
        useCurrentLocation: Boolean,
        manualLocation: ManualLocationInput
    ): Boolean {
        var lat: Double? = null
        var lng: Double? = null
        if (useCurrentLocation) {
            lastKnownLocation(context)?.let { location ->
                lat = location.latitude
                lng = location.longitude
            }
        }

        val queryText = buildMapsQueryText(baseQuery, useCurrentLocation, manualLocation)
        val primaryUri = if (useCurrentLocation && lat != null && lng != null) {
            Uri.parse("geo:$lat,$lng?q=${Uri.encode(baseQuery.trim())}")
        } else {
            Uri.parse(buildMapsWebUrl(queryText))
        }

        val primaryIntent = Intent(Intent.ACTION_VIEW, primaryUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(primaryIntent)
            true
        }.getOrElse {
            val fallback = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(buildMapsWebUrl(queryText))
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching {
                context.startActivity(fallback)
                true
            }.getOrDefault(false)
        }
    }

    fun openWebSearch(
        context: Context,
        queryText: String
    ): Boolean {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(buildWebSearchUrl(queryText))
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun dialPhoneNumber(context: Context, rawPhone: String): Boolean {
        val sanitized = rawPhone.trim().filter { it.isDigit() || it == '+' || it == '-' || it == ' ' }
        if (sanitized.isBlank()) return false
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openExternalHttpUrl(context: Context, rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return false
        val normalized = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            !trimmed.contains("://") -> "https://$trimmed"
            else -> return false // Block non-HTTP(S) schemes such as javascript:/file:
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(context: Context): Location? {
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!coarseGranted && !fineGranted) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = buildList {
            if (coarseGranted || fineGranted) add(LocationManager.NETWORK_PROVIDER)
            if (fineGranted) add(LocationManager.GPS_PROVIDER)
        }
        return providers.firstNotNullOfOrNull { provider ->
            try {
                locationManager.getLastKnownLocation(provider)
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
