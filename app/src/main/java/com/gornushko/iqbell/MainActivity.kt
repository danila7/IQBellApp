package com.gornushko.iqbell

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    var outStream: OutputStream? = null
    private val REQUEST_ENABLE_BLUETOOTH = 1
    lateinit var mHandler: Handler

    companion object {
        val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val address = "C2:2C:05:04:04:FA"
        val TAG = "Bluetooth Action"
    }

    private val mBroadcastReceiver1 = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action!!
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    var state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG, "BT STATE ON!")
                            btIsOn()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "Bt STATE OFF!")
                            btIsOff()
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            Log.d(TAG, "BT STATE TURNING OFF...")
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            Log.d(TAG, "BT STATE TURNING ON...")
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Toast.makeText(this, getString(R.string.bt_not_supported), Toast.LENGTH_LONG).show()
            finish()
        }
        var btIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver1, btIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver1)
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
        Log.d(TAG, "Button Turn On")
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(enableBtIntent)
    }

    private fun btIsOff() {
        btStateText.text = getText(R.string.bt_is_off)
        bt_on_button.visibility = View.VISIBLE
        device_status.visibility = View.GONE
        connect_button.visibility = View.GONE
        connection_status.visibility = View.GONE
    }

    private fun btIsOn() {
        btStateText.text = getText(R.string.bt_is_on)
        bt_on_button.visibility = View.GONE
        device_status.visibility = View.VISIBLE
        checkIfPaired()
    }

    private fun checkIfPaired() {
        var isPaired = false
        for (device: BluetoothDevice in btAdapter!!.bondedDevices) if (device.address == address) isPaired = true
        if (isPaired) {
            device_status.text = getText(R.string.paired)
            connect_button.visibility = View.VISIBLE
        } else {
            device_status.text = getText(R.string.unpaired)
        }
    }

    fun connect(view: View){
        val connectThread = ConnectThread(btAdapter!!.getRemoteDevice(address))
        mHandler = @SuppressLint("HandlerLeak")
        object : Handler() {

            override fun handleMessage(msg: Message?) {
                connection_status.visibility = View.VISIBLE
                when(msg!!.what){
                    0 -> connection_status.text = getText(R.string.disconnected)
                    1 -> connection_status.text = getText(R.string.connected)
                }
            }
        }
        connectThread.start()
    }

    private fun checkBt() {
        connect_button.visibility = View.GONE
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

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try{
                    socket.connect()
                    Log.d(TAG, "Connected?")
                } catch (e: IOException){}

                if(socket.isConnected) mHandler.sendEmptyMessage(1)
                else mHandler.sendEmptyMessage(0)


                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                //manageMyConnectedSocket(socket)
            }
            cancel()
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
                Log.d(TAG,"Socket closed")
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }
}
/*
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter!!.cancelDiscovery()

            mmSocket?.use { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.

            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Toast.makeText(baseContext, "Could not close the client socket", Toast.LENGTH_LONG).show()
            }
        }
    }

}
*/
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
