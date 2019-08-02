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


class MainActivity : AppCompatActivity(), UIControl {
    override fun btOff() {
        btService?.closeConnection()
        status.text = getText(R.string.bt_is_off)
        bt_on_button.visibility = View.VISIBLE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.INVISIBLE
        btService?.closeConnection()
        btService = null
    }

    override fun btOnNotPaired() {
        status.text = getText(R.string.not_paired)
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.INVISIBLE
    }

    override fun connected() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.VISIBLE
        password.visibility = View.VISIBLE
        progress.visibility = View.INVISIBLE
        status.text = getText(R.string.connected)
    }

    override fun reconnecting() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.VISIBLE
        status.text = getText(R.string.disconnected)
    }

    override fun authenticating() {
        login_button.isEnabled = true
        progress.visibility = View.VISIBLE

    }

    override fun authFailed() {
        status.text = getText(R.string.authorization_failed)
        progress.visibility = View.GONE
    }

    override fun authSucceed() {
        progress.visibility = View.GONE
        val intent = Intent(this, WorkActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    override fun connecting() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.VISIBLE
        status.text = getText(R.string.connecting)
        btService?.start()
    }

    private var btAdapter: BluetoothAdapter? = null
    private var btService: BluetoothClass? = null

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
                            checkBt()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            Log.d(TAG, "BT STATE OFF!")
                            status.text = getText(R.string.bt_is_off)
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            Log.d(TAG, "BT STATE TURNING OFF...")
                            status.text = getText(R.string.bt_is_turning_off)
                            btOff()
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {
                            Log.d(TAG, "BT STATE TURNING ON...")
                            status.text = getText(R.string.bt_is_turning_on)

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
        val connectHandler = @SuppressLint("HandlerLeak")
        object : Handler() {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    0 -> {
                        reconnecting()
                    }
                    1 -> {
                        connected()
                    }
                }
            }
        }
        btService = BluetoothClass(btAdapter!!, connectHandler, this)
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

    private fun checkBt() {
        if (btAdapter!!.isEnabled) {
            var isPaired = false
            for (device: BluetoothDevice in btAdapter!!.bondedDevices) if (device.address == address) isPaired = true
            if (isPaired) {
                connecting()
            } else {
                btOnNotPaired()
            }
        } else {
            btOff()
        }
    }

    override fun onStart() {
        super.onStart()
        checkBt()

    }

    fun login(view: View) {
        if (password.text.toString().length == 16) {
            val pass = password.text.toString().toByteArray(Charset.forName("ASCII"))
            btService?.authorize(pass)
        } else authFailed()
    }
}
