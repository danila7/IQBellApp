package com.gornushko.iqbell

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Message
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class BluetoothService(val btAdapter: BluetoothAdapter, val handler: android.os.Handler) {

    companion object {
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val address = MainActivity.address
        const val TAG = "My Bluetooth Service"
        var socket: BluetoothSocket? = null
    }

    private val connectThread: ConnectThread

    init {
        connectThread = ConnectThread()
        connectThread.start()
    }

    fun closeConnection(){
        connectThread.cancel()
    }

    private inner class ConnectThread : Thread() {

        private val device = btAdapter.getRemoteDevice(address)
        private val mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID)

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter.cancelDiscovery()
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            try{
                mmSocket.connect()
                Log.d(TAG, "Connected?")
            } catch (e: IOException){
                Log.e(TAG, "An error occurred during connection process: " + e.message)
            }
            if(mmSocket.isConnected){
                handler.sendEmptyMessage(1)
            }
            else{
                handler.sendEmptyMessage(0)
                cancel()
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket.close()
                Log.d(TAG,"Socket closed")
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket: " + e.message)
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
}