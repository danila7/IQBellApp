package com.gornushko.iqbell

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
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
    }

    private var data: DataThread? = null
    private val connectThread: ConnectThread
    var inputData: ByteArray? = null


    init {
        connectThread = ConnectThread()
        connectThread.start()
    }

    fun startDataThread(){
        data?.start()
    }

    fun closeConnection(){
        connectThread.cancel()
        data?.cancel()
    }

    fun write(bytes: ByteArray){
        data?.write(bytes)
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
                //cancel()
            }

            if(mmSocket.isConnected){
                data = DataThread(mmSocket)
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
                Log.e(TAG, "Could not cancel the client socket", e)
            }
        }
    }


    private inner class DataThread(socket: BluetoothSocket): Thread(){
        val oStream: OutputStream
        val iStream: InputStream
        init {
            Log.d(TAG, "Connected Thread: Starting...")
            oStream = socket.outputStream
            iStream = socket.inputStream
        }

        override fun run() {
            while (true) {
                inputData = try {
                    if(inputData == null) iStream.readBytes()
                    else{
                        if(inputData != null){
                            val newData = iStream.readBytes()
                            val oldData: ByteArray = inputData!!
                            oldData + newData
                        } else iStream.readBytes()
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to read data from Input Stream")
                    break
                }
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
                Log.e(TAG, "Failed to write data to output stream")
            }
        }

        fun cancel(){
            try{
                iStream.close()
                oStream.close()
            }catch(e: IOException){}
        }
    }
}