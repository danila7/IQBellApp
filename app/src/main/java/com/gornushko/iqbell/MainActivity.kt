package com.gornushko.iqbell

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import java.nio.charset.Charset

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(){

    companion object Const{
        private const val TAG = "Bluetooth Action"
        private const val SAVED_PASSWORD = "saved_password"
        const val KEY = "key"
    }

    private var loggingIn = false
    private var notPaired = false

    private fun btOff() {
        status.text = getText(R.string.bt_is_off)
        bt_on_button.visibility = View.VISIBLE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.INVISIBLE
        pass_info.visibility = View.GONE
        save_pass.visibility = View.GONE
        turnBtOn(bt_on_button)
    }

    private fun btOnNotPaired() {
        notPaired = true
        status.text = getText(R.string.not_paired)
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        pass_info.visibility = View.GONE
        save_pass.visibility = View.GONE
        progress.visibility = View.INVISIBLE
    }

    private fun connected() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.VISIBLE
        password.visibility = View.VISIBLE
        pass_info.visibility = View.VISIBLE
        save_pass.visibility = View.VISIBLE
        progress.visibility = View.INVISIBLE
        status.text = getText(R.string.connected)
        val sPref = getPreferences(Context.MODE_PRIVATE)
        val pass = sPref.getString(SAVED_PASSWORD, "0")
        if(pass != null && pass.length > 7
        ){
            password.editText!!.setText(pass)
        }
    }

    private fun reconnecting() {
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.VISIBLE
        pass_info.visibility = View.GONE
        save_pass.visibility = View.GONE
        status.text = getText(R.string.disconnected)
    }

    private fun authenticating() {
        login_button.isEnabled = true
        progress.visibility = View.VISIBLE

    }

    private fun authFailed() {
        pass_info.text = getText(R.string.authorization_failed)
        pass_info.setTextColor(Color.RED)
        progress.visibility = View.INVISIBLE
    }

    private fun authSucceed() {
        progress.visibility = View.GONE
        if(save_pass.isChecked){
            val ed = getPreferences(Context.MODE_PRIVATE).edit()
            ed.putString(SAVED_PASSWORD, password.editText!!.text.toString())
            ed.apply()
            toast(getString(R.string.pass_saved))
        }
        loggingIn = true
        startActivity(intentFor<WorkActivity>().newTask().clearTask().clearTop())
    }

    private fun connecting() {
        notPaired = false
        bt_on_button.visibility = View.GONE
        login_button.visibility = View.GONE
        password.visibility = View.GONE
        progress.visibility = View.VISIBLE
        pass_info.visibility = View.GONE
        save_pass.visibility = View.GONE
        status.text = getText(R.string.connecting)
    }

    private fun btNotSupported(){
        bt_on_button.visibility = View.GONE
        status.text = getText(R.string.bt_not_supported)
        progress.visibility = View.GONE
        password.visibility = View.GONE
        login_button.visibility = View.GONE
        pass_info.visibility = View.GONE
        save_pass.visibility = View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

    override fun onRestart() {
        super.onRestart()
        if(notPaired) startService(intentFor<IQService>(IQService.ACTION to IQService.CHECK_PAIRED))
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
        if (password.editText!!.text. toString().length == 8) {
            val pass = password.editText!!.text.toString().toByteArray(Charset.forName("ASCII"))
            startService(intentFor<IQService>(IQService.ACTION to IQService.AUTH, IQService.PASSWORD to pass))
        } else authFailed()
    }

    fun turnBtOn(view: View) = startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))

    fun savePass(view: View){
        if(!save_pass.isChecked){
            val ed = getPreferences(Context.MODE_PRIVATE).edit()
            ed.putString(SAVED_PASSWORD, "0")
            ed.apply()
            toast(getString(R.string.pass_deleted))
        }
    }
}
