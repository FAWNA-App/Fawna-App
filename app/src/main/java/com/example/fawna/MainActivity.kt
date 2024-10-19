package com.example.fawna

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.fawna.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bleNodeManager: BleNodeManager

    private val PERMISSIONS_REQUEST_CODE = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private lateinit var messagesTextView: TextView
    private lateinit var newMessageEditText: EditText
    private lateinit var sendMessageButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val navView: BottomNavigationView = binding.navView

//        val navController = findNavController(R.id.nav_host_fragment_activity_main)
//        val appBarConfiguration = AppBarConfiguration(
//            setOf(R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_settings)
//        )
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navView.setupWithNavController(navController)

        // Initialize UI components
        messagesTextView = binding.messagesTextView // Assume you have this in your layout
        newMessageEditText = binding.newMessageEditText // Assume you have this in your layout
        sendMessageButton = binding.sendMessageButton // Assume you have this in your layout

        // Set up the send message button
        sendMessageButton.setOnClickListener { sendMessage() }

        // Check for Bluetooth permissions
        checkPermissions()
    }

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, missingPermissions.toTypedArray(), PERMISSIONS_REQUEST_CODE
            )
        } else {
            initializeBluetooth()
        }
    }

    private fun initializeBluetooth() {
        bleNodeManager = BleNodeManager(this)
        bleNodeManager.start()

        // Connect to the ESP32 bulletin board device
//        bleNodeManager.connectToDevice("ESP32_S3_NimBLE") // Replace with your ESP32 device name
    }

    private fun sendMessage() {
        val newMessage = newMessageEditText.text.toString()
        if (newMessage.isNotBlank()) {
            bleNodeManager.sendPost(newMessage) // Implement this in BleNodeManager
            newMessageEditText.text.clear()
            readMessages()
        }
    }

    private fun readMessages() {
        val messages = bleNodeManager.readPosts() // Implement this in BleNodeManager
        messagesTextView.text = messages.joinToString("\n") // Update the UI with the messages
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetooth()
            } else {
                // Permissions denied - notify user or disable Bluetooth functionality
            }
        }
    }
}
