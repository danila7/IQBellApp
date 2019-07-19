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

    companion object {
        var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var address = "C2:2C:05:04:04:FA"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (btAdapter!!.isEnabled) {
                    btIsOn()
                } else {
                    Toast.makeText(this, getText(R.string.error_bt_enabling), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun turnBtOn(view: View) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
    }

    private fun btIsOff() {
        btStateText.text = getText(R.string.bt_is_off)
        bt_on_button.visibility = View.VISIBLE
        device_status.visibility = View.GONE
    }

    private fun btIsOn() {
        btStateText.text = getText(R.string.bt_is_on)
        bt_on_button.visibility = View.GONE
        device_status.visibility = View.VISIBLE
        checkIfPaired()
    }

    private fun checkIfPaired(){
        var isPaired = false
        for (device: BluetoothDevice in btAdapter!!.bondedDevices) if (device.address == address) isPaired = true
        if (isPaired) {
            device_status.text = getText(R.string.paired)
            check_connection_button.visibility = View.VISIBLE
            createBtSocket()
        } else {
            device_status.text = getText(R.string.unpaired)
        }
    }

    private fun createBtSocket() {
        val btDevice = btAdapter!!.getRemoteDevice(address)

        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(myUUID)
        } catch (e: IOException) {
            Toast.makeText(this, "Fatal Error: Socket create failed", Toast.LENGTH_LONG).show()
        }
        btAdapter!!.cancelDiscovery()
        try {
            btSocket!!.connect()
        } catch (e: IOException) {
            try {
                btSocket!!.close()
            } catch (e2: IOException) {
                Toast.makeText(this, "Fatal Error: Unable to close socket during connection failure", Toast.LENGTH_LONG)
                    .show()
            }
        }
        try {
            outStream = btSocket!!.outputStream
        } catch (e: IOException) {
            Toast.makeText(this, "Fatal Error: Output stream creation failed", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun checkBt() {
        check_connection_button.visibility = View.GONE
        connection_status.visibility = View.GONE
        if (btAdapter!!.isEnabled) {
            btIsOn()
        } else {
            btIsOff()
        }
    }

    public override fun onResume() {
        super.onResume()
        checkBt()
    }

    fun checkConnection(view: View) {
        connection_status.visibility = View.VISIBLE
        connection_status.text = if (btSocket!!.isConnected) getText(R.string.connected) else getText(R.string.disconnected)
    }
}

/*
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
    */
