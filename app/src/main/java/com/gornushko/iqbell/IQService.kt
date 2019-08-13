package com.gornushko.iqbell

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.jetbrains.anko.doAsync
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.util.*
import android.app.NotificationManager
import android.app.NotificationChannel
import java.io.OutputStream
import java.util.zip.CRC32


@ExperimentalUnsignedTypes
class IQService: Service() {

    private var pi: PendingIntent? = null
    private var btAdapter: BluetoothAdapter? = null
    private var connectThread: StartConnectionThread
    private var connection: ConnectionThread
    private var socket: BluetoothSocket? = null
    private lateinit var oStream: OutputStream
    private lateinit var iStream: InputStream

    init {
        connectThread = StartConnectionThread()
        connection = ConnectionThread()
    }

    companion object {
        private val PASSWORD = byteArrayOf(0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38)
        const val address = "C2:2C:05:04:04:FA"
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "My Bluetooth Service"
        private const val NOTIFICATIONS_CHANNEL = "new_channel"
        const val BT_OFF = 400
        const val BT_NOT_SUPPORTED = 1
        const val PENDING_INTENT = "pint"
        const val ACTION = "action"
        const val START = 1
        const val CHECK_PAIRED = 2
        const val CONNECTION = 3
        const val RECONNECTING = 5
        const val CONNECTED = 6
        const val NOT_PAIRED = 4
        const val STOP_SERVICE = 404
        const val NEW_PENDING_INTENT = 301
        const val SEND_DATA = 22
        const val DEVICE_STATE = 23
        const val DATA = "dt"
        const val EXTRA_DATA = "edt"
        const val GET_EXTRA_DATA = 8
        const val NEW_EXTRA = 9
    }

    override fun onCreate() {
        super.onCreate()
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        val btIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver, btIntent)
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent!!.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_ON -> checkPaired()
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            stop()
                            pi?.send(BT_OFF)
                            updateNotification(getString(R.string.preparing), true)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent!!.getIntExtra(ACTION, START)) {
            START -> {
                pi = intent.getParcelableExtra(PENDING_INTENT)
                if (btAdapter == null) {
                    pi?.send(BT_NOT_SUPPORTED)
                    stopSelf()
                } else {
                    updateNotification(getString(R.string.preparing), true)
                    checkBt()
                }
            }
            NEW_PENDING_INTENT -> {
                pi = intent.getParcelableExtra(PENDING_INTENT)
            }
            CHECK_PAIRED -> if (btAdapter!!.isEnabled) checkPaired()
            STOP_SERVICE -> {
                stop()
                stopForeground(true)
                stopSelf()
            }
            SEND_DATA -> {
                //sendData(intent.getIntExtra(TASK, 0), intent.getByteArrayExtra(DATA)!!)
            }
            GET_EXTRA_DATA -> connection.getExtraDataFlag = true
        }
        return START_NOT_STICKY
    }

    private fun updateNotification(text: String, progress: Boolean) {
        createNotificationsChannel()
        val builder = NotificationCompat.Builder(this, NOTIFICATIONS_CHANNEL)
            .setSmallIcon(R.drawable.ic_bell_outline)
            .setContentTitle(getText(R.string.iq_status))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null, AudioManager.STREAM_RING)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        if (progress) builder.setProgress(0, 0, true)
        val notification = builder.build()
        startForeground(1, notification)
    }

    private fun createNotificationsChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                NOTIFICATIONS_CHANNEL, "Connection status",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "IQBell Status"
            channel.enableVibration(false)
            channel.enableLights(false)
            channel.lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        return null
    }

    private fun checkBt() {
        if (!btAdapter!!.isEnabled) {
            pi?.send(BT_OFF)
            updateNotification(getString(R.string.preparing), true)
        } else checkPaired()
    }

    fun checkPaired() {
        var isPaired = false
        for (device: BluetoothDevice in btAdapter!!.bondedDevices) if (device.address == address) isPaired = true
        if (isPaired) {
            pi?.send(CONNECTION)
            updateNotification(getString(R.string.connecting), true)
            connection.start()
        } else {
            pi?.send(NOT_PAIRED)
            updateNotification(getString(R.string.preparing), true)
        }
    }

    fun stop() {
        doAsync {
            connection.runningFlag = false
            connection.interrupt()
            connection.join()
            connectThread.runningFlag = false
            connectThread.interrupt()
            connectThread.join()
            connection.runningFlag = false
            connection.interrupt()
            connection.join()
            try {
                socket?.close()
            } catch (e: IOException) {}
            Log.d(TAG, "FINISHED")
        }
    }

    private inner class StartConnectionThread : Thread() {
        var runningFlag = true
        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter?.cancelDiscovery()
            while (runningFlag) {
                var mmSocket: BluetoothSocket
                try {
                    mmSocket = btAdapter?.getRemoteDevice(address)?.createRfcommSocketToServiceRecord(MY_UUID) ?: continue
                } catch (e: Exception) {continue}
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    mmSocket.connect()
                } catch (e: IOException) {}
                if (mmSocket.isConnected) {
                    socket = mmSocket
                    iStream = mmSocket.inputStream
                    oStream = mmSocket.outputStream
                    var tempExtraData: ByteArray? = null
                    var tempData: ByteArray? = null
                    for(i in 0..10){
                        tempExtraData = getData(true)
                        if(tempExtraData != null) break
                    }
                    for(i in 0..5){
                        tempData = getData()
                        if(tempData != null) break
                    }
                    if(tempData == null) tempData = ByteArray(5)
                    if(tempExtraData == null) tempExtraData = ByteArray(80)
                    updateNotification(getString(R.string.connected), false)
                    pi?.send(applicationContext, CONNECTED, Intent().putExtra(DATA, tempData).putExtra(EXTRA_DATA, tempExtraData))
                    break
                } else {
                    pi?.send(RECONNECTING)
                    updateNotification(getString(R.string.connecting), true)
                    try {
                        mmSocket.close()
                        Log.d(TAG, "Socket closed")
                    } catch (e: IOException) {
                        Log.e(TAG, "Could not close the client socket: " + e.message)
                    }
                }
                try {
                    sleep(200)
                } catch (e: InterruptedException) {}
            }
        }
    }

    private inner class ConnectionThread : Thread() {
        var counter = 0
        var runningFlag = true
        var getExtraDataFlag = false
        override fun run() {
            while (runningFlag) {
                if (!connectThread.isAlive) {
                    if (socket?.isConnected != true) {
                        connectThread = StartConnectionThread()
                        connectThread.start()
                    } else {
                        try {
                            val data = getData()
                            if (data != null) { //if authorization succeed
                                counter = 0
                                pi?.send(applicationContext, DEVICE_STATE, Intent().putExtra(DATA, data))
                            } else counter++
                            if (counter == 3) {
                                counter = 0
                                pi?.send(RECONNECTING)
                                updateNotification(getString(R.string.connecting), true)
                                connectThread = StartConnectionThread()
                                connectThread.start()
                            }
                        } catch (e: Exception) {
                            pi?.send(RECONNECTING)
                            updateNotification(getString(R.string.connecting), true)
                            connectThread = StartConnectionThread()
                            connectThread.start()
                        }
                        if(getExtraDataFlag){
                            getExtraDataFlag = false
                            var tempExtraData: ByteArray? = null
                            for(i in 0..10){
                                tempExtraData = getData(true)
                                if(tempExtraData != null) break
                            }
                            pi?.send(applicationContext, NEW_EXTRA, Intent().putExtra(EXTRA_DATA, tempExtraData))
                        }
                    }
                }
                try {
                    sleep(670)
                } catch (e: InterruptedException) {}
            }
        }
    }
/*
    private fun sendData(task: Int, data: ByteArray){ //an universal function for receiving/sending data
        doAsync {
            Log.d(TAG, "Started async task doTransfer()")
            connection?.runningFlag = false //interrupting connection thread
            connection?.join()
            Log.d(TAG, "Joined")
            try{
                val iStream = socket!!.inputStream
                val oStream = socket!!.outputStream
                oStream?.write(PASSWORD)  //authorization
                oStream?.flush()
                Thread.sleep(100) //waiting for answer
                var code = 0
                if(iStream.available() > 0) {
                    code = iStream.read() //reading the answer
                    clearInput(iStream)
                }
                Log.d(TAG, "code is: $code")
                if(code == 11){ //if authorization succeed
                    oStream.write(data)
                    val crc = CRC32()
                    crc.reset()
                    crc.update(data)
                    sendChecksum(crc.value, oStream) //sending checksum
                    Thread.sleep(150)
                    Log.d(TAG, "Data available: ${iStream.available()}")
                    if(iStream.available() > 0) {
                        val nByte = iStream.read()
                        Log.d(TAG, "nByte: $nByte")
                        if (nByte == 11) Log.d(TAG, "Success")
                        else Log.d(TAG, "The data was corrupted during sending to Arduino")
                    }
                }
            } catch (e: Exception){}
            restartPing()
        }
    }*/

    private fun clearInput(str: InputStream) {
        while (str.available() > 0) str.read()
    }

    private fun sendChecksum(sum: Long, stream: OutputStream) {
        val tempSum = sum.toUInt()
        for (i in 0..3) stream.write((tempSum shr 8 * i).toUByte().toInt())
        stream.flush()
    }

    private fun getChecksum(stream: InputStream): Long {
        val tempData = ByteArray(4)
        stream.read(tempData)
        var result = 0u
        for (i in 3 downTo 0) result = (result shl 8) + tempData[i].toUByte()
        return result.toLong()
    }
/*
    private fun restartPing(){
        connection = null
        connection = ConnectionThread()
        connection!!.start()
    }*/

    private fun connect(): Boolean {
        try{
            oStream.write(PASSWORD)  //authorization
            oStream.flush()
        }catch (e: IOException){}
        try{
            Thread.sleep(100) //waiting for answer
        }catch (e: InterruptedException){}
        var code = 0
        if (iStream.available() > 0) {
            code = iStream.read() //reading the answer
            clearInput(iStream)
        }
        return code == 11
    }

    private fun getData(extra: Boolean = false): ByteArray? {
        if(!connect()) return null
        val myData: ByteArray = byteArrayOf(if(extra) 0x5 else 0x0, 0x6e, 0x75, 0x6c, 0x6c)
        oStream.write(myData)
        val crc = CRC32()
        crc.reset()
        crc.update(myData)
        sendChecksum(crc.value, oStream) //sending checksum
        Thread.sleep(200)
        if (iStream.available() > 0) {
            val nByte = iStream.read()
            if (nByte == 11) {
                crc.reset()
                crc.update(nByte)
                val tempData = ByteArray(if(extra) 80 else 5)
                iStream.read(tempData)
                crc.update(tempData)
                val gCh = getChecksum(iStream)
                val sCh = crc.value
                if (gCh ==sCh) return tempData
            }
        }
        return null
    }
}