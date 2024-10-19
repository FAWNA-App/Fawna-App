package com.example.fawna

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

// Custom UUID for Bluetooth Low Energy
const val baseBluetoothUuidPostfix = "0000-1000-BEEF-00805F9B34FC"

/**
 * Creates a 16-bit UUID from a short code.
 *
 * Args:
 *     shortCode16: The 16-bit short code for the UUID.
 *
 * Returns:
 *     A UUID corresponding to the 16-bit short code.
 */
fun uuidFromShortCode16(shortCode16: String): UUID {
    return UUID.fromString("0000$shortCode16-$baseBluetoothUuidPostfix")
}

/**
 * Creates a 32-bit UUID from a short code.
 *
 * Args:
 *     shortCode32: The 32-bit short code for the UUID.
 *
 * Returns:
 *     A UUID corresponding to the 32-bit short code.
 */
fun uuidFromShortCode32(shortCode32: String): UUID {
    return UUID.fromString("$shortCode32-$baseBluetoothUuidPostfix")
}

class BleNodeManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    // Using 16-bit UUIDs
    private val serviceUuid: UUID = uuidFromShortCode16("0001") // Replace with your service UUID
    private val characteristicUuid: UUID = uuidFromShortCode16("0002") // Replace with your characteristic UUID

    private val processedMessages = mutableSetOf<String>()
    private val connectedDevices = mutableListOf<BluetoothGatt>()

    private var isCentralMode = true
    private val roleSwitchInterval: Long = 5000 // Switch every 5 seconds
    private val roleSwitchTimer = Timer()

    private val discoveredDevices = mutableMapOf<String, Int>() // Device address to hop count

    init {
        // Check for required permissions when the class is instantiated
        if (!hasPermissions()) {
            Log.e("BleNodeManager", "Missing required Bluetooth permissions.")
        }
    }

    fun start() {
        startRoleSwitching()
        startScanning()
    }

    fun getDiscoveredDevices(): Map<String, Int> {
        return discoveredDevices
    }

    // Check for required permissions before starting Bluetooth features
    private fun hasPermissions(): Boolean {
        val scanPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        val connectPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        val advertisePermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        val fineLocationPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return scanPermission && connectPermission && advertisePermission && fineLocationPermission
    }

    // Start advertising if permissions are granted
    fun startAdvertising() {
        if (!isCentralMode) {  // Check if we are in peripheral mode
            Log.w("BleNodeManager", "Already in peripheral mode, skipping advertising")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BleNodeManager", "Bluetooth advertise permission not granted.")
            return
        }

        try {
            if (bluetoothAdapter?.isEnabled == true) {
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true)
                    .build()

                val data = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(serviceUuid))
                    .setIncludeDeviceName(false)
                    .build()

                advertiser?.startAdvertising(settings, data, advertiseCallback)
                Log.i("BleNodeManager", "Advertising started successfully")
            } else {
                Log.e("BleNodeManager", "Bluetooth is not enabled.")
            }
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth advertise permission.")
        }
    }

    // Start scanning if permissions are granted
    private fun startScanning() {
        if (isCentralMode) {  // Ensure we are in central mode before scanning
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BleNodeManager", "Bluetooth scan permission not granted.")
                return
            }

            try {
                // Create a scan filter to match devices by advertised name or UUID
                val nameFilter = ScanFilter.Builder()
                    .setDeviceName("ESP32_S3_NimBLE") // Filter devices with name containing "ESP32"
                    .build()

//                val uuidFilter = ScanFilter.Builder()
//                    .setServiceUuid(ParcelUuid(serviceUuid)) // 16-bit UUID
//                    .build()

                val scanSettings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                // Start scanning with both filters
//                scanner?.startScan(listOf(nameFilter, uuidFilter), scanSettings, scanCallback)
                scanner?.startScan(listOf(nameFilter), scanSettings, scanCallback)
                Log.i("BleNodeManager", "Scanning started successfully")
            } catch (e: SecurityException) {
                Log.e("BleNodeManager", "SecurityException: Missing Bluetooth scan permission.")
            }
        } else {
            Log.w("BleNodeManager", "Already in central mode, skipping scanning (this should not happen)")
        }
    }


    // Stop advertising if already active
    private fun stopAdvertising() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth advertise permission.")
        }
        Log.i("BleNodeManager", "Advertising stopped")
    }

    // Stop scanning if already scanning
    private fun stopScanning() {
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth scan permission.")
        }
        Log.i("BleNodeManager", "Scanning stopped")
    }

    // Start role switching
    private fun startRoleSwitching() {
        roleSwitchTimer.schedule(object : TimerTask() {
            override fun run() {
                switchRole()
            }
        }, 0, roleSwitchInterval) // Using schedule instead of scheduleAtFixedRate
    }

    // Switch between central and peripheral roles
    private fun switchRole() {
        try {
            if (isCentralMode) {
                stopScanning()
                startAdvertising()
            } else {
                stopAdvertising()
                startScanning()
            }
            isCentralMode = !isCentralMode
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
        }
    }

    // Create a message with ID, source device ID, timestamp, hop count, and content
    fun createMessage(sourceDeviceId: String, content: String, hopCount: Int = 0): String {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        return "$messageId:$sourceDeviceId:$timestamp:$hopCount:$content"
    }

    // Send a message to a connected device
    fun sendMessage(message: String, gatt: BluetoothGatt) {
        try {
            val service = gatt.getService(serviceUuid)
            if (service == null) {
                Log.e("BleNodeManager", "Service not found for UUID: $serviceUuid")
                return
            }
            val characteristic = service.getCharacteristic(characteristicUuid) // Null pointer exception here on "service"
            characteristic.value = message.toByteArray(Charsets.UTF_8)
            gatt.writeCharacteristic(characteristic)
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
        }
    }

    // Extract message ID from a message string
    private fun extractMessageId(message: String): String {
        return message.split(":")[0]
    }

    // Relay a message to connected devices, excluding the sender
    private fun relayMessageToNeighbors(message: String, senderGatt: BluetoothGatt) {
        for (connectedDevice in connectedDevices) {
            if (connectedDevice != senderGatt) {
                // Increment hop count here and update the map
                val hopCount = message.split(":")[3].toInt()
                discoveredDevices[connectedDevice.device.address] = hopCount + 1
                val service = connectedDevice.getService(serviceUuid)
                val characteristic = service.getCharacteristic(characteristicUuid)
                characteristic.value = message.toByteArray(Charsets.UTF_8)
                try {
                    connectedDevice.writeCharacteristic(characteristic)
                    Log.i("BleNodeManager", "Message sent to device: ${connectedDevice.device.address}")
                } catch (e: SecurityException) {
                    Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
                }
            }
        }
    }

    // Advertise callback for Bluetooth LE
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i("BleNodeManager", "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BleNodeManager", "Advertising failed with error code: $errorCode")
        }
    }

    // Scan callback for Bluetooth LE
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                if (hasPermissions()) {
                    Log.i("BleNodeManager", "Discovered device: ${device.address}, attempting to connect.")
                    Log.i("BleNodeManager", "Discovered device: ${device.address}, attempting to connect.")
                    connectToDevice(device)  // Use the new method
                } else {
                    Log.e("BleNodeManager", "Permissions not granted for connecting.")
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult?>) {
            results.forEach { result ->
                result?.device?.let { device ->
                    if (hasPermissions()) {
                        Log.i("BleNodeManager", "Discovered device: ${device.address}, attempting to connect.")
                        connectToDevice(device)  // Use the new method
                    } else {
                        Log.e("BleNodeManager", "Permissions not granted for connecting.")
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleNodeManager", "Scan failed with error code: $errorCode")
        }
    }

    // Connect to a device
    private fun connectToDevice(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BleNodeManager", "Bluetooth connect permission not granted.")
            return
        }

        try {
            device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
        }
    }

    // GATT callback for managing connections
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("BleNodeManager", "Connected to device: ${gatt.device.address}")
                    connectedDevices.add(gatt)
                    try {
                        gatt.discoverServices()
                    } catch (e: SecurityException) {
                        Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i("BleNodeManager", "Disconnected from device: ${gatt.device.address}")
                    connectedDevices.remove(gatt)
                    try {
                        gatt.close()
                    } catch (e: SecurityException) {
                        Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleNodeManager", "Services discovered for device: ${gatt.device.address}")
            } else {
                Log.e("BleNodeManager", "Failed to discover services for device: ${gatt.device.address}, status: $status")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleNodeManager", "Characteristic written successfully to device: ${gatt.device.address}")
            } else {
                Log.e("BleNodeManager", "Failed to write characteristic to device: ${gatt.device.address}, status: $status")
            }
        }
    }

    /**
     * Sends a message to connected devices.
     *
     * Args:
     *     message: The message to be sent.
     */
    fun sendPost(message: String) {
        // Create a message with a unique ID and hop count
        try {
            val sourceDeviceId = bluetoothAdapter?.name ?: "UnknownDevice"
            val messageToSend = createMessage(sourceDeviceId, message)

            // Relay the message to connected devices
            for (connectedDevice in connectedDevices) {
                Log.i("BleNodeManager", "Relaying message to device: ${connectedDevice.device.name} ${connectedDevice.device.address}")
                sendMessage(messageToSend, connectedDevice)
            }

            // Store the processed message to prevent duplication
            processedMessages.add(messageToSend)
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
            return
        }
    }

    /**
     * Reads the processed messages.
     *
     * Returns:
     *     A list of messages that have been processed.
     */
    fun readPosts(): List<String> {
        return processedMessages.toList() // Return a copy of the processed messages
    }
}
