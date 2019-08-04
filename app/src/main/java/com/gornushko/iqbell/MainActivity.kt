package com.gornushko.iqbell

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset

class MainActivity : AppCompatActivity(){

    companion object Const{
        private const val TAG = "Bluetooth Action"
    }

    private var loggingIn = false
    private fun btOff() {
        status.text = getText(R.string.bt_is_off)
        bt_on_button.visibility = View.VISIBLE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.INVISIBLE
        turnBtOn(bt_on_button)
    }

    private fun btOnNotPaired() {
        status.text = getText(R.string.not_paired)
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.INVISIBLE
    }

    private fun connected() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.VISIBLE
        password.visibility = View.VISIBLE
        progress.visibility = View.INVISIBLE
        status.text = getText(R.string.connected)
    }

    private fun reconnecting() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.VISIBLE
        status.text = getText(R.string.disconnected)
    }

    private fun authenticating() {
        login_button.isEnabled = true
        progress.visibility = View.VISIBLE

    }

    private fun authFailed() {
        status.text = getText(R.string.authorization_failed)
        progress.visibility = View.GONE
    }

    private fun authSucceed() {
        progress.visibility = View.GONE
        val intent = Intent(this, WorkActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        loggingIn = true
        startActivity(intent)
    }

    private fun connecting() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.VISIBLE
        status.text = getText(R.string.connecting)
    }

    private fun btNotSupported(){
        bt_on_button.visibility = View.GONE
        status.text = getText(R.string.bt_not_supported)
        progress.visibility = View.GONE
        password.visibility = View.GONE
        login_button.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pIntent = createPendingResult(1, intent, 0)
        val launchServiceIntent = Intent(this, IQService::class.java)
            .putExtra(IQService.ACTION, IQService.START)
            .putExtra(IQService.PENDING_INTENT, pIntent)
        startService(launchServiceIntent)
    }

    override fun onRestart() {
        super.onRestart()
        startService(Intent(this, IQService::class.java).putExtra(IQService.ACTION, IQService.CHECK_PAIRED))
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!loggingIn) {
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
            IQService.CONNECTED -> connected()
            IQService.AUTH_PROCESS -> authenticating()
            IQService.AUTH_FAILED -> authFailed()
            IQService.AUTH_SUCCEED -> authSucceed()
        }
    }

    fun login(view: View) {
        if (password.text.toString().length == 16) {
            val pass = password.text.toString().toByteArray(Charset.forName("ASCII"))
            Log.d(TAG, "LOGIN")
            startService(Intent(this, IQService::class.java)
                .putExtra(IQService.ACTION, IQService.AUTH)
                .putExtra(IQService.PASSWORD, pass))
        } else authFailed()
    }

    fun turnBtOn(view: View) = startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
}
