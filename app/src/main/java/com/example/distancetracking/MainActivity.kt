package com.example.distancetracking

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.example.distancetracking.Permission.Permissions.hasLocationPermission
import com.example.distancetracking.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        if (hasLocationPermission(this)) {
            findNavController(R.id.navController).navigate(R.id.action_permissionFragment_to_mapsFragment)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}