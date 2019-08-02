package com.gornushko.iqbell

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.lang.Exception
import java.util.*

class BluetoothClass(val btAdapter: BluetoothAdapter, val handler: android.os.Handler, val ui: UIControl) {

    companion object {
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val address = MainActivity.address
        const val TAG = "My Bluetooth Service"
        var socket: BluetoothSocket? = null
    }

    private var connectThread: ConnectThread
    private var check: CheckConnectionThread? = null


    init {
        connectThread = ConnectThread()
    }

    fun start() {
        check = CheckConnectionThread()
        check!!.start()
    }

    fun stop() {
        check?.interrupt()
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

    fun authorize(pass: ByteArray){
        val auth = Authorization(ui)
        auth.execute(pass)
    }

    private inner class ConnectThread: Thread() {

        private val device = btAdapter.getRemoteDevice(address)

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter.cancelDiscovery()
            while (!interrupted()) {
                Log.d(TAG, "Attempt to connect...")
                val mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
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
                    handler.sendEmptyMessage(1)
                    break
                } else {
                    handler.sendEmptyMessage(0)
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
                                handler.sendEmptyMessage(0)
                                Log.d(TAG, "counter!!!")
                                connectThread = ConnectThread()
                                connectThread.start()
                            }
                        } catch (e: Exception) {
                            handler.sendEmptyMessage(0)
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

    @SuppressLint("StaticFieldLeak")
    inner class Authorization(val ui: UIControl) : AsyncTask<ByteArray, Void, Boolean>() {

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
            ui.authenticating()
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            if(result!!) ui.authSucceed()
            else ui.authFailed()
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
