package com.example.fawna

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

// Custom UUID for Bluetooth Low Energy
const val baseBluetoothUuidPostfix = "0000-1000-BEEF-00805F9B34FC"

fun uuidFromShortCode16(shortCode16: String): UUID {
    return UUID.fromString("0000$shortCode16-$baseBluetoothUuidPostfix")
}

fun uuidFromShortCode32(shortCode32: String): UUID {
    return UUID.fromString("$shortCode32-$baseBluetoothUuidPostfix")
}

class BleNodeManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val serviceUuid: UUID = uuidFromShortCode16("0001") // Replace with your service UUID
    private val characteristicUuid: UUID = uuidFromShortCode16("0001") // Replace with your characteristic UUID

    private val processedMessages = mutableSetOf<String>()
    private val connectedDevices = mutableListOf<BluetoothGatt>()

    private var isCentralMode = true
    private val roleSwitchInterval: Long = 5000 // Switch every 5 seconds
    private val roleSwitchTimer = Timer()

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
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BleNodeManager", "Bluetooth advertise permission not granted.")
                return
            }

            if (bluetoothAdapter?.isEnabled == true) {
                // Stop existing advertising before starting a new one
                advertiser?.stopAdvertising(advertiseCallback)

                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(true)
                    .build()

                val data = AdvertiseData.Builder()
                    .addServiceUuid(ParcelUuid(serviceUuid))
                    .setIncludeDeviceName(true)
                    .build()

                // Start advertising
                advertiser?.let {
                    it.startAdvertising(settings, data, advertiseCallback)
                } ?: Log.e("BleNodeManager", "BluetoothLeAdvertiser is null.")
            } else {
                Log.e("BleNodeManager", "Bluetooth is not enabled.")
            }
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth advertise permission.")
        }
    }


    // Start scanning if permissions are granted
    fun startScanning() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BleNodeManager", "Bluetooth scan permission not granted.")
                return
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(serviceUuid))
                .build()

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            // Debug: Start scanning without the filter to isolate the crash
            scanner?.startScan(null, scanSettings, scanCallback)

//            scanner?.startScan(listOf(filter), scanSettings, scanCallback) // Cause of crash at 12:29 AM (Fix this crash)
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth scan permission.")
        }
    }

    // Start role switching
    fun startRoleSwitching() {
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
                scanner?.stopScan(scanCallback)
                startAdvertising()
            } else {
                advertiser?.stopAdvertising(advertiseCallback)
                startScanning()
            }
            isCentralMode = !isCentralMode
        } catch (e: SecurityException) {
            Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
        }
    }

    // Create a message with ID, source device ID, timestamp, and content
    fun createMessage(sourceDeviceId: String, content: String): String {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        return "$messageId:$sourceDeviceId:$timestamp:$content"
    }

    // Send a message to a connected device
    fun sendMessage(message: String, gatt: BluetoothGatt) {
        try {
            val service = gatt.getService(serviceUuid)
            val characteristic = service.getCharacteristic(characteristicUuid)
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

    // Advertise callback
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i("BleNodeManager", "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e("BleNodeManager", "Advertising start failed: $errorCode")
        }
    }

    // Scan callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                if (hasPermissions()) {
                    Log.i("BleNodeManager", "Discovered device: ${device.address}, attempting to connect.")
                    try {
                        device.connectGatt(context, false, gattCallback)
                    } catch (e: SecurityException) {
                        Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
                    }
                } else {
                    Log.e("BleNodeManager", "Permissions not granted for connecting.")
                }
            }
        }

        override fun onBatchScanResults(results: List<ScanResult?>?) {
            // Handle batch scan results if needed
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BleNodeManager", "Scan failed: $errorCode")
        }
    }

    // GATT callback
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("BleNodeManager", "Connected to device: ${gatt.device.address}")
                connectedDevices.add(gatt)
                try {
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    Log.e("BleNodeManager", "SecurityException: Missing Bluetooth permissions.")
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("BleNodeManager", "Disconnected from device: ${gatt.device.address}")
                connectedDevices.remove(gatt)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleNodeManager", "Services discovered on device: ${gatt.device.address}")
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == characteristicUuid) {
                val message = characteristic.value.toString(Charsets.UTF_8)
                val messageId = extractMessageId(message)

                if (!processedMessages.contains(messageId)) {
                    processedMessages.add(messageId)
                    relayMessageToNeighbors(message, gatt)
                    Log.i("BleNodeManager", "Received message: $message")
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BleNodeManager", "Message sent successfully")
            }
        }
    }
}
