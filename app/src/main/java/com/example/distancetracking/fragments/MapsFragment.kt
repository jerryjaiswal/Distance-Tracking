package com.example.distancetracking.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.distancetracking.Permission.Constants.ACTION_SERVICE_START
import com.example.distancetracking.Permission.Constants.ACTION_SERVICE_STOP
import com.example.distancetracking.Permission.Extension.disable
import com.example.distancetracking.Permission.Extension.enable
import com.example.distancetracking.Permission.Extension.hide
import com.example.distancetracking.Permission.Extension.show
import com.example.distancetracking.Permission.MapUtil.calculateElapsedTime
import com.example.distancetracking.Permission.MapUtil.distanceTraveled
import com.example.distancetracking.Permission.MapUtil.setCameraPosition
import com.example.distancetracking.Permission.Permissions.hasBackgroundPermission
import com.example.distancetracking.Permission.Permissions.requestBackgroundPermission
import com.example.distancetracking.R
import com.example.distancetracking.databinding.FragmentMapsBinding
import com.example.distancetracking.model.Result
import com.example.distancetracking.service.Tracker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions

class MapsFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
    EasyPermissions.PermissionCallbacks, GoogleMap.OnMarkerClickListener {

    private var _binding: FragmentMapsBinding? = null
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap
    private var locationList = mutableListOf<LatLng>()
    private var starTime = 0L
    private var stopTime = 0L
    var started = MutableLiveData(false)
    private var polylineList = mutableListOf<Polyline>()
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerList = mutableListOf<Marker>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)

//user latest and last known location
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        binding.start.setOnClickListener {
//            here we want to start tracking the location of the user
            onStartButtonClicked()
        }

        binding.stop.setOnClickListener {
//            here the user wants to stop tracking the location
            onStopButtonClicked()
        }

        binding.reset.setOnClickListener {
            onResetButtonClicked()
        }

        return binding.root
    }

    private fun onStartButtonClicked() {

        if (hasBackgroundPermission(requireContext())) {
            startCountDown()
            binding.start.disable()
            binding.start.hide()
            binding.stop.show()
        } else {
            requestBackgroundPermission(this)
        }
    }

    private fun onStopButtonClicked() {
//        stop the foreground service of tracking the location
        sendActionCommandtoService(ACTION_SERVICE_STOP)
//        binding.start.show()
//        binding.start.enable()
//        binding.stop.disable()
//        binding.stop.hide()
    }


    private fun onResetButtonClicked() {
        mapReset()
    }

    private fun startCountDown() {
        binding.timerText.show()
        binding.stop.disable()
        val timer: CountDownTimer = object : CountDownTimer(4000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if (currentSecond.toString() == "0") {
                    binding.timerText.text = "GO"
                    binding.timerText.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.black
                        )
                    )
                } else {
                    binding.timerText.text = currentSecond.toString()
                }
            }

            override fun onFinish() {
                sendActionCommandtoService(ACTION_SERVICE_START)
                binding.timerText.hide()
            }
        }
        timer.start()
    }


    //    this will send the signal to the serivce to start or not
    private fun sendActionCommandtoService(action: String) {
        Intent(requireContext(), Tracker::class.java).apply {
            this.action = action

//            when we start the service the onStartCommand will triggered
            requireContext().startService(this)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isMyLocationEnabled = true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
//        map.uiSettings.apply {
//            isZoomControlsEnabled = ture
//            isZoomGesturesEnabled = false
//            isRotateGesturesEnabled = false
//            isTiltGesturesEnabled = false
//            isCompassEnabled = false
//            isScrollGesturesEnabled = false
//        }
        observeTrackerService()
        if (started.value == true) {
            binding.stop.show()
            binding.tapOnText.hide()
        }


        val pdpu = LatLng(23.15660593850999, 72.66003369908326)
        val bhaijiPura = LatLng(23.161936568283334, 72.63047274283362);
        val shreeRangPlaza = LatLng(23.18508535229741, 72.65185679917526);

        addMarker(pdpu, "PDPU")
        addMarker(bhaijiPura, "Bhaiji Pura")
        addMarker(shreeRangPlaza, "Shree Rang Plaza")
    }

    private fun addMarker(position: LatLng, s: String) {
        map.addMarker(
            MarkerOptions().position(position).title(s)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )
        map.moveCamera(CameraUpdateFactory.newLatLng(position))

    }


    override fun onMyLocationButtonClick(): Boolean {
        binding.tapOnText.animate().alpha(0f).duration = 1500
        lifecycleScope.launch {
            delay(2500)
            binding.tapOnText.hide()
            binding.start.show()
        }
        return false
    }

    @SuppressLint("MissingPermission")
    private fun mapReset() {
        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation = LatLng(
                it.result.latitude, it.result.longitude
            )
            for (polyline in polylineList) {
                polyline.remove()
            }
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(lastKnownLocation)
                )
            )
            locationList.clear()
            for (marker in markerList) {
                marker.remove()
            }
            markerList.clear()
            binding.reset.hide()
            binding.start.show()
        }
    }

    private fun observeTrackerService() {
        Tracker.locationLiveData.observe(viewLifecycleOwner) {
            if (it != null) {
                locationList = it

                if (locationList.size > 1) {
                    binding.stop.enable()
                }
                drawPolyline()
                followPolyline()
            }
        }
        Tracker.started.observe(viewLifecycleOwner) {
            started.value = it
        }

        Tracker.startTime.observe(viewLifecycleOwner) {
            starTime = it
        }
        Tracker.stopTime.observe(viewLifecycleOwner) {
            stopTime = it
            if (stopTime != 0L) {
                showBiggerPicture()
                showResult()
            }
        }
    }

    private fun followPolyline() {
        if (locationList.isNotEmpty()) {
            map.animateCamera(
                (CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(
                        locationList.last()
                    )
                )), 1000, null
            )
        }
    }

    private fun drawPolyline() {
        val polyline = map.addPolyline(
            PolylineOptions().apply {
                width(10f)
                color(Color.BLUE)
                startCap(ButtCap())
                endCap(ButtCap())
                jointType(JointType.ROUND)
                addAll(locationList)
            }
        )
        polylineList.add(polyline)
    }


    //    to show the whole path which is covered by the user
    private fun showBiggerPicture() {
        val bounds = LatLngBounds.Builder()
        for (location in locationList) {
            bounds.include(location)
        }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 500), 2000, null)
        addMarker(locationList.first())
        addMarker(locationList.last())
    }

    private fun addMarker(position: LatLng) {
        val marker = map.addMarker(MarkerOptions().position(position))
        marker?.let { markerList.add(it) }
    }

    private fun showResult() {
        val result = Result(
            distanceTraveled(locationList),
            calculateElapsedTime(starTime, stopTime)
        )
        lifecycleScope.launch {
            delay(2500)
            val directions = MapsFragmentDirections.actionMapsFragmentToResultFragment(result)
            findNavController().navigate(directions)

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        Log.d("main", "onPermissionsGranted: permission granted")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            Log.d("main", "onPermissionsGranted: permission requested")
        } else {
            requestBackgroundPermission(this)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }
}