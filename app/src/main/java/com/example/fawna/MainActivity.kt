package com.example.fawna

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fawna.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var messages: MutableList<Message>
    private lateinit var messageAdapter: MessageAdapter
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var esp32Device: BluetoothDevice? = null
    private var bluetoothSocket: BluetoothSocket? = null

    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val ESP32_MAC_ADDRESS = "48:27:E2:1E:07:F1" // Replace with your ESP32's MAC address
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

        setupBluetooth()
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
            sendMessageToESP32(message)
        }
    }

    private fun sendMessageToESP32(message: String) {
        if (bluetoothSocket?.isConnected == true) {
            try {
                bluetoothSocket?.outputStream?.write(message.toByteArray())
                addMessage("Posted: $message", true)
            } catch (e: IOException) {
                Toast.makeText(this, "Failed to post message", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Not connected to ESP32", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_LONG).show()
            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            connectToESP32()
        }
    }

    private fun connectToESP32() {
        esp32Device = bluetoothAdapter?.getRemoteDevice(ESP32_MAC_ADDRESS)

        try {
            bluetoothSocket = esp32Device?.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket?.connect()
            Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
//            Toast.makeText(this, "Failed to connect to ESP32", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close()
    }
}