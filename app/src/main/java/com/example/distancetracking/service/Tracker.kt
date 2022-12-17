package com.example.distancetracking.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.distancetracking.Permission.Constants.ACTION_SERVICE_START
import com.example.distancetracking.Permission.Constants.ACTION_SERVICE_STOP
import com.example.distancetracking.Permission.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.distancetracking.Permission.Constants.LOCATION_UPDATE_INTERVAL
import com.example.distancetracking.Permission.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetracking.Permission.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.distancetracking.Permission.Constants.NOTIFICATION_ID
import com.example.distancetracking.Permission.MapUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/*
what is service : -
1. app component that can perform long running operation
2. once started, can continue for some time even the user switches to another app

types : -
1. foregound : - that is noticable to user eg: - audio tag, non-removable notification
2. background : - non noticabel to user
3. bound : - client server interface, live only upto the server bound to it, when all the outer sources are
become unbound then the service is destroyed
*/


//onBind() : - when we call bindService() method then system invokes the onBind(),
//we use it when another computer want ot bind with the service

//the blocking tasks should be done into the other thread as it will affect the service


@AndroidEntryPoint
class Tracker : LifecycleService() {
    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var notification: NotificationCompat.Builder
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient


    companion object {
        var started = MutableLiveData<Boolean>()
        val locationLiveData = MutableLiveData<MutableList<LatLng>>()
        val startTime = MutableLiveData<Long>()
        val stopTime = MutableLiveData<Long>()
    }


    private fun setInitialValues() {
        started.postValue(false)
        locationLiveData.postValue(mutableListOf())
        startTime.postValue(0)
        stopTime.postValue(0)
    }


    private fun updateLocationList(location: Location) {
        val newLatLng = LatLng(location.latitude, location.longitude)
        locationLiveData.value?.apply {
            add(newLatLng)
            locationLiveData.postValue(this)
        }
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result?.locations?.let { locations ->
                for (location in locations) {
                    Log.d("main", "onLocationResult: $location")
                    updateLocationList(location)
                    updateNotificationPeriodically()
                }
            }
        }
    }


    //    the system invokes this method to perform one-time setup
//        before onStartCommand() or onBind(), basically used to create the service
    override fun onCreate() {
        setInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }


    //    when we call startService() method then this function method is triggerd
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_SERVICE_START -> {
                    startForegroundService()
                    startLocationUpdates()
                    started.postValue(true)
                }
                ACTION_SERVICE_STOP -> {
                    started.postValue(false)
                    stopForegoundService()
                }
            }


        }

        return super.onStartCommand(intent, flags, startId)
    }


    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification.build())
    }

    private fun stopForegoundService() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        stopForeground(true)
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
            Priority.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
        startTime.postValue(System.currentTimeMillis())
    }


    private fun updateNotificationPeriodically() {
        notification.apply {
            setContentTitle("Distance Travelled")
            setContentText(locationLiveData.value?.let {
                MapUtil.distanceTraveled(it)
            } + "km")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

    }


}
