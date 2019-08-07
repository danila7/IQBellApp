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
                getTime(data!!.getLongExtra("date", 0))

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

    private fun getTime(timestamp: Long){
        val cal = GregorianCalendar.getInstance()
        cal.timeInMillis = timestamp
        val timeString = "${cal.get(Calendar.HOUR)}:${cal.get(Calendar.MINUTE)}:${cal.get(Calendar.SECOND)}"
        val dateString = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH)+1}/${cal.get(Calendar.YEAR)}"
        current_time.text = timeString
        current_date.text = dateString

    }
}
