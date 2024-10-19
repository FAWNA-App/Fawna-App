package com.example.fawna

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fawna.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var messages: MutableList<Message>
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var bleNodeManager: BleNodeManager

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        messages = mutableListOf()
        messageAdapter = MessageAdapter(this, messages)
        binding.messagesListView.adapter = messageAdapter

        // Add some test messages
        addMessage("Welcome to the local postboard!", false)
        addMessage("This is a test message", true)

        binding.sendMessageButton.setOnClickListener { sendMessage() }

        initializeBluetooth()
    }

    private fun addMessage(content: String, isLocal: Boolean) {
        messages.add(Message(content, isLocal))
        messageAdapter.notifyDataSetChanged()
    }

    private fun sendMessage() {
        val message = binding.newMessageEditText.text.toString().trim()
        if (message.isNotEmpty()) {
            addMessage(message, true)
            binding.newMessageEditText.setText("")
            sendMessageToBleNode(message)
        }
    }

    private fun sendMessageToBleNode(message: String) {
        if (bleNodeManager.hasPermissions()) {
            try {
                bleNodeManager.sendPost(message)
                Log.i("MainActivity", "Message sent: $message")
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to post message", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Bluetooth permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeBluetooth() {
        bleNodeManager = BleNodeManager(this)

        if (!bleNodeManager.hasPermissions()) {
            requestPermissions(arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ), PERMISSIONS_REQUEST_CODE)
        } else {
            bleNodeManager.start() // Start scanning or advertising based on the current mode
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetooth()
            } else {
                Toast.makeText(this, "Permissions denied. Bluetooth will not function.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleNodeManager.stopAdvertising()
    }
}
