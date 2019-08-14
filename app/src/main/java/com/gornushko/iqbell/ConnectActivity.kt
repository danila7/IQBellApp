package com.gornushko.iqbell

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_connect.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

@ExperimentalUnsignedTypes
class ConnectActivity : AppCompatActivity(){

    companion object Const{
        private const val TAG = "IQBell Login Activity"
        const val KEY = "key"
    }

    private var connected = false
    private var notPaired = false

    private fun btOff() {
        status.text = getText(R.string.bt_is_off)
        bt_on_button.visibility = View.VISIBLE
        progress.visibility = View.INVISIBLE
        turnBtOn()
    }

    private fun btOnNotPaired() {
        notPaired = true
        status.text = getText(R.string.not_paired)
        bt_on_button.visibility = View.GONE
        progress.visibility = View.INVISIBLE
    }

    private fun connected(data: ByteArray) {
        connected = true
        startActivity(intentFor<MainActivity>(MainActivity.START_DATA to data).newTask().clearTask().clearTop())
    }

    private fun reconnecting() {
        bt_on_button.visibility = View.GONE
        progress.visibility = View.VISIBLE
        status.text = getText(R.string.disconnected)
    }

    private fun connecting() {
        notPaired = false
        bt_on_button.visibility = View.GONE
        progress.visibility = View.VISIBLE
        status.text = getText(R.string.connecting)
    }

    private fun btNotSupported(){
        bt_on_button.visibility = View.GONE
        status.text = getText(R.string.bt_not_supported)
        progress.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connect)
        bt_on_button.onClick { turnBtOn() }
        setSupportActionBar(toolbar as Toolbar?)
        when(intent.extras?.getInt(KEY)){
            IQService.RECONNECTING ->{
                startService(intentFor<IQService>(IQService.ACTION to IQService.NEW_PENDING_INTENT, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
                reconnecting()
            }
            IQService.BT_OFF -> {
                startService(intentFor<IQService>(IQService.ACTION to IQService.NEW_PENDING_INTENT, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
                btOff()
            }
            else -> startService(intentFor<IQService>(IQService.ACTION to IQService.START, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_about -> startActivity(intentFor<AboutActivity>())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRestart() {
        super.onRestart()
        if(notPaired) startService(intentFor<IQService>(IQService.ACTION to IQService.CHECK_PAIRED))
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!connected) {
            startService(Intent(this, IQService::class.java).putExtra(IQService.ACTION, IQService.STOP_SERVICE))
        }
        Log.d(TAG, "MAIN ACTIVITY DESTROYED")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Yeah! request: $requestCode result: $resultCode")
        when(resultCode){
            IQService.CONNECTION -> connecting()
            IQService.NOT_PAIRED -> btOnNotPaired()
            IQService.BT_OFF -> btOff()
            IQService.BT_NOT_SUPPORTED -> btNotSupported()
            IQService.RECONNECTING -> reconnecting()
            IQService.CONNECTED -> connected(data!!.getByteArrayExtra(IQService.DATA)!!)
        }
    }


    private fun turnBtOn() = startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))

}
