package com.gornushko.iqbell

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_work.*
import org.jetbrains.anko.*
import java.util.*

@ExperimentalUnsignedTypes
class WorkActivity : AppCompatActivity() {

    companion object Const{
        private const val TAG = "Bluetooth Action"
    }

    private var goingBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)
        startService(intentFor<IQService>(IQService.ACTION to IQService.NEW_PENDING_INTENT, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Yeah! request: $requestCode result: $resultCode")
        when(resultCode){
            IQService.GOT_INFO -> {
                val deviceTime = data!!.getLongExtra("date", 0)
                val deviceDate = Date(deviceTime)
                device_info.text = deviceDate.toString()
            }
            IQService.RECONNECTING, IQService.BT_OFF -> {
                goingBack = true
                startActivity(intentFor<MainActivity>(MainActivity.KEY to resultCode).newTask().clearTask().clearTop())
            }
        }
    }

    fun getInfo(view: View){
        startService(Intent(this, IQService::class.java).putExtra(IQService.ACTION, IQService.GET_INFO))
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!goingBack) startService(intentFor<IQService>(IQService.ACTION to IQService.STOP_SERVICE))
    }
}
