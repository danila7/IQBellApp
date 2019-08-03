package com.gornushko.iqbell

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log

class IQService: Service() {

    private var pi: PendingIntent? = null
    private var btAdapter: BluetoothAdapter? = null

    companion object{
        const val address = "C2:2C:05:04:04:FA"
        private const val TAG = "MY BLUETOOTH SERVICE"
        const val BT_OFF = 400
        const val BT_NOT_SUPPORTED = 1
        const val PENDING_INTENT = "pint"
        const val ACTION = "action"
        const val START = 1
        const val CHECK_PAIRED = 2
        const val PAIRED = 3
        const val NOT_PAIRED = 4
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
        if (isPaired) pi?.send(PAIRED)
        else pi?.send(NOT_PAIRED)
    }
}