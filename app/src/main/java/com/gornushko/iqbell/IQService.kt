package com.gornushko.iqbell

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.IBinder
import android.util.Log
import java.io.IOException
import java.lang.Exception
import java.util.*

class IQService: Service() {

    private var pi: PendingIntent? = null
    private var btAdapter: BluetoothAdapter? = null
    private var connectThread: ConnectThread
    private var check: CheckConnectionThread? = null
    private var socket: BluetoothSocket? = null

    init {
        connectThread = ConnectThread()
    }

    companion object{
        const val address = "C2:2C:05:04:04:FA"
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val TAG = "My Bluetooth Service"
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
        const val AUTH = 7
        const val PASSWORD = "pass"
        const val AUTH_SUCCEED = 12
        const val AUTH_FAILED = 10
        const val AUTH_PROCESS = 11
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        val btIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver, btIntent)
    }

    private val mBroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent!!.action){
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)){
                        BluetoothAdapter.STATE_ON -> checkPaired()
                        BluetoothAdapter.STATE_TURNING_OFF -> pi?.send(BT_OFF)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        unregisterReceiver(mBroadcastReceiver)
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Start command")
        when(intent!!.getIntExtra(ACTION, START)){
            START ->{
                pi = intent.getParcelableExtra(PENDING_INTENT)
                if (btAdapter == null) {
                    pi?.send(BT_NOT_SUPPORTED)
                    stopSelf()
                } else{
                    checkBt()
                }
            }
            CHECK_PAIRED -> if(btAdapter!!.isEnabled) checkPaired()
            AUTH -> authorize(intent.getByteArrayExtra(PASSWORD)!!)
        }
        return START_NOT_STICKY

    }

    override fun onBind(p0: Intent?): IBinder? {
        Log.d(TAG, "onBind")
        return null
    }

    private fun checkBt(){
        if (!btAdapter!!.isEnabled) pi?.send(BT_OFF)
        else checkPaired()
    }

    fun checkPaired(){
        var isPaired = false
        for (device: BluetoothDevice in btAdapter!!.bondedDevices) if (device.address == address) isPaired = true
        if (isPaired){
            pi?.send(CONNECTION)
            start()
        }
        else pi?.send(NOT_PAIRED)
    }

    private fun start(){
        check = CheckConnectionThread()
        check!!.start()
    }

    fun stop() {
        check?.flag = false
        check = null
    }

    fun closeConnection() {
        connectThread.interrupt()
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the client socket: " + e.message)
        }
    }

    private inner class ConnectThread: Thread() {

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter!!.cancelDiscovery()
            while (!interrupted()) {
                Log.d(TAG, "Attempt to connect...")
                val mmSocket = btAdapter!!.getRemoteDevice(address).createRfcommSocketToServiceRecord(MY_UUID)
                try {
                    // Connect to the remote device through the socket. This call blocks
                    // until it succeeds or throws an exception.
                    mmSocket.connect()
                    Log.d(TAG, "Connected?")
                } catch (e: IOException) {
                    Log.d(TAG, "An error occurred during connection process: " + e.message)
                }
                if (mmSocket.isConnected) {
                    socket = mmSocket
                    pi?.send(CONNECTED)
                    break
                } else {
                    pi?.send(RECONNECTING)
                    try {
                        mmSocket.close()
                        Log.d(TAG, "Socket closed")
                    } catch (e: IOException) {
                        Log.e(TAG, "Could not close the client socket: " + e.message)
                    }
                }
                sleep(200)
            }
        }
    }

    private inner class CheckConnectionThread: Thread(){
        var counter = 0
        var flag = true
        override fun run() {
            while (flag) {
                if (!connectThread.isAlive) {
                    if (socket?.isConnected != true) {
                        Log.d(TAG, "socket not equals true!")
                        connectThread = ConnectThread()
                        connectThread.start()
                    }
                    if (socket?.isConnected == true) {
                        try {
                            val oStream = socket!!.outputStream
                            val iStream = socket!!.inputStream
                            oStream.write(1)
                            oStream.flush()
                            sleep(100)
                            if (iStream.available() > 0) {
                                if (iStream.read() == 5) {
                                    counter = 0
                                } else counter++
                                iStream.skip(iStream.available().toLong())
                            } else counter++
                            if (counter == 3) {
                                counter = 0
                                pi?.send(RECONNECTING)
                                Log.d(TAG, "counter!!!")
                                connectThread = ConnectThread()
                                connectThread.start()
                            }
                        } catch (e: Exception) {
                            pi?.send(RECONNECTING)
                            connectThread = ConnectThread()
                            connectThread.start()
                        }
                    }
                }
                try {
                    sleep(200)
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Interrupted")
                }
            }
        }
    }

    fun authorize(pass: ByteArray){
        val auth = Authorization()
        auth.execute(pass)
    }

    @SuppressLint("StaticFieldLeak")
    private inner class Authorization : AsyncTask<ByteArray, Void, Boolean>() {

        override fun doInBackground(vararg p0: ByteArray?): Boolean {
            check?.flag = false
            Log.d(TAG, "Interrupt succeed")
            check?.join()
            Log.d(TAG, "Joined")
            val oStream = socket!!.outputStream
            val iStream = socket!!.inputStream
            try{
                oStream.write(p0[0]!!)
            }catch (e: IOException){}
            Thread.sleep(1_000)
            var code = 0
            if(iStream.available() > 0){
                code = iStream.read()
                while (iStream.available() > 0) iStream.read()
            }
            check = null
            check = CheckConnectionThread()
            check!!.start()
            return (code == 82)
        }

        override fun onPreExecute() {
            super.onPreExecute()
            pi?.send(AUTH_PROCESS)
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            pi?.send(if(result!!) AUTH_SUCCEED else AUTH_FAILED)
        }
    }
}

/*
    private inner class DataThread(socket: BluetoothSocket): Thread(){
        val oStream: OutputStream
        val iStream: InputStream
        init {
            Log.d(TAG, "Connected Thread: Starting...")
            oStream = socket.outputStream
            iStream = socket.inputStream
        }

        override fun run() {
            Log.d(TAG,"Starting input thread listening")
            while (true) {




                Log.d(TAG, "Checking input...")
                if(iStream.available() > 0) try{
                    Log.i(TAG, "Data was received")
                    val av = iStream.available()
                    Log.d(TAG, "$av bytes available")
                    var iData = ByteArray(av)
                    iStream.read(iData)
                    handler.sendMessage(Message.obtain(handler, 2, iData))
                } catch (e: IOException){
                    Log.e(TAG, "Can't read connection from Input Stream")
                }
                sleep(1000)
            }
        }

        fun write(bytes: ByteArray){
            val text = String(bytes, Charset.defaultCharset())
            Log.d(TAG, "Writing to output stream: $text")
            try {
                oStream.write(bytes)
                oStream.flush()
                Log.d(TAG, "Success")
            } catch (e: IOException){
                Log.e(TAG, "Failed to write connection to output stream")
            }
        }

        fun cancel(){
            try{
                iStream.close()
                oStream.close()
            }catch(e: IOException){}
        }
    }*/
