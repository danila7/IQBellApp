package com.gornushko.iqbell

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
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
import java.nio.charset.Charset


class MainActivity : AppCompatActivity() {

    private var btAdapter: BluetoothAdapter? = null
    private var isConnected: Boolean = false
    private var btService: BluetoothService? = null
    private var mAuth: Authorization? = null

    companion object {
        private const val TAG = "Bluetooth Action"
        const val address = "C2:2C:05:04:04:FA"
    }

    private val mBroadcastReceiver1 = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action!!) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_ON -> {
                            Log.d(TAG, "BT STATE ON!")
                            btIsOn()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "BT STATE OFF!")
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            Log.d(TAG, "BT STATE TURNING OFF...")
                            bt_state_text.text = getText(R.string.bt_is_turning_off)
                            btIsOff()
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            Log.d(TAG, "BT STATE TURNING ON...")
                            bt_state_text.text = getText(R.string.bt_is_turning_on)

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
        val btIntent = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBroadcastReceiver1, btIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver1)
        btService?.closeConnection()
    }

    fun turnBtOn(view: View) {
        Log.d(TAG, "Button Turn On")
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivity(enableBtIntent)
    }

    private fun btIsOff() {
        bt_state_text.text = getText(R.string.bt_is_off)
        bt_on_button.visibility = View.VISIBLE
        device_status.visibility = View.GONE
        connect_button.visibility = View.GONE
        connection_status.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        if(mAuth != null) mAuth!!.cancel(true)
        btService?.closeConnection()
        btService = null
        progress.visibility = View.INVISIBLE
    }

    private fun btIsOn() {
        bt_state_text.text = getText(R.string.bt_is_on)
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
        connect_button.visibility = View.GONE
        connection_status.visibility = View.INVISIBLE
        progress.visibility = View.VISIBLE
        val connectHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    0 -> {
                        connection_status.text = getText(R.string.disconnected)
                        progress.visibility = View.INVISIBLE
                        connection_status.visibility = View.VISIBLE
                        login_button.visibility = View.GONE
                        login_button.visibility = View.GONE
                        connect_button.visibility = View.VISIBLE

                    }
                    1 -> {
                        connection_status.text = getText(R.string.connected)
                        progress.visibility = View.INVISIBLE
                        connection_status.visibility = View.VISIBLE
                        isConnected = true
                        login_button.visibility = View.VISIBLE
                        password.visibility = View.VISIBLE
                        connect_button.visibility = View.GONE
                    }
                }
            }
        }
        btService = BluetoothService(btAdapter!!, connectHandler)
    }

    private fun checkBt() {
        if(!isConnected) {
            login_button.visibility = View.GONE
            password.visibility = View.GONE
        }
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

    fun login(view: View){
        if(password.text.toString().length == 16) {
            val pass = password.text.toString().toByteArray(Charset.forName("ASCII"))
            mAuth = Authorization(this)
            mAuth!!.execute(pass)
        }
        else Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
        //btService!!.write(send_text.text.toString().toByteArray(Charset.forName("ASCII")))
    }

}
