package com.gornushko.iqbell

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket?  = null
    var outStream: OutputStream? = null
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object{
        var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var address = "C2:2C:05:04:04:FA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        refresh_button.setOnClickListener {pairedDeviceList()}
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            errorExit("Fatal Error", "Bluetooth is not supported")
        }
        if (!btAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        }
    }

    private fun pairedDeviceList() {
        for (device: BluetoothDevice in btAdapter!!.bondedDevices) {
            if (device.address == address) {
                   Toast.makeText(this, "The device is paired", Toast.LENGTH_SHORT).show()
               }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode ==  REQUEST_ENABLE_BLUETOOTH){
            if(resultCode == Activity.RESULT_OK){
                if(btAdapter!!.isEnabled){
                    Toast.makeText(this,  "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                } else{
                    Toast.makeText(this,  "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
                }
            } else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(this,  "Bluetooth enabling has been canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        // Set up a pointer to the remote node using it's address.
        val device = btAdapter!!.getRemoteDevice(address)
        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.
        try {
            btSocket = device.createRfcommSocketToServiceRecord(myUUID)
        } catch (e: IOException) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.message + ".")
        }
        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter!!.cancelDiscovery()
        // Establish the connection.  This will block until it connects.
        try {
            btSocket!!.connect()
        } catch (e: IOException) {
            try {
                btSocket!!.close()
            } catch (e2: IOException) {
                errorExit(
                    "Fatal Error",
                    "In onResume() and unable to close socket during connection failure" + e2.message + "."
                )
            }
        }
        try {
            outStream = btSocket!!.outputStream
        } catch (e: IOException) {
            errorExit("Fatal Error", "In onResume() and output stream creation failed:" + e.message + ".")
        }
    }

    public override fun onPause() {
        super.onPause()
        if (outStream != null) {
            try {
                outStream!!.flush()
            } catch (e: IOException) {
                errorExit("Fatal Error", "In onPause() and failed to flush output stream: " + e.message + ".")
            }
        }
        try {
            btSocket!!.close()
        } catch (e2: IOException) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.message + ".")
        }
    }


    private fun errorExit(title: String, message: String) {
        Toast.makeText(this, "$title - $message", Toast.LENGTH_LONG).show()
        finish()
    }

    fun checkConnection(view: View){
        if (btSocket!!.isConnected){
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show()
        } else{
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendData(message: String) {
        val msgBuffer = message.toByteArray()
        try {
            outStream!!.write(msgBuffer)
            Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error sending data", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendOn(view: View){
        sendData("1")
    }

    fun sendOff(view: View){
        sendData("0")
    }
}
