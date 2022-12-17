package com.example.distancetracking.Permission


import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.DecimalFormat

object MapUtil {
    fun setCameraPosition(location: LatLng): CameraPosition {
        return CameraPosition.Builder().target(location).zoom(18f).build()
    }


    //    these two function are for the result fragment
    fun calculateElapsedTime(startTime: Long, stopTime: Long): String {
        val elapsedTime = stopTime - startTime
        val seconds = (elapsedTime / 1000).toInt() % 60
        val minutes = (elapsedTime / (1000 * 60) % 60)
        val hours = (elapsedTime / (1000 * 60 * 60) % 24)
        return "$hours:$minutes:$seconds"
    }

    //util library is used to find distance
    fun distanceTraveled(location: MutableList<LatLng>): String {
        if (location.size > 1) {
            val meters = SphericalUtil.computeDistanceBetween(location.first(), location.last())
            val kilometers = meters / 1000
            return DecimalFormat("#.##").format(kilometers)
        }
        return "0.00"
    }
}