package com.gornushko.iqbell

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_work.*
import org.jetbrains.anko.*
import java.text.DateFormat
import java.util.*

@ExperimentalUnsignedTypes
class WorkActivity : AppCompatActivity() {

    companion object Const{
        private const val TAG = "Bluetooth Action"
    }

    private var goingBack = false
    private val currentDateTime: Calendar = GregorianCalendar.getInstance()
    private val newDateTime: Calendar = GregorianCalendar.getInstance()

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
        currentDateTime.timeInMillis = timestamp
        current_time.text = DateFormat.getTimeInstance().format(currentDateTime.time)
        current_date.text = DateFormat.getDateInstance().format(currentDateTime.time)

    }

    fun timePicker(view: View){
        val dialog = TimePickerDialog(this, android.R.style.ThemeOverlay_Material_Dialog,
            TimePickerDialog.OnTimeSetListener{_, mHour, mMinute -> run{
                newDateTime.set(Calendar.HOUR_OF_DAY, mHour)
                newDateTime.set(Calendar.MINUTE, mMinute)
                newDateTime.set(Calendar.SECOND, 0)
                new_time.text = DateFormat.getTimeInstance().format(newDateTime.time)
            }}, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true)
        dialog.show()
    }

    fun datePicker(view: View){
        val dialog = DatePickerDialog(this, android.R.style.ThemeOverlay_Material_Dialog,
            DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay -> run{
                newDateTime.set(Calendar.YEAR, mYear)
                newDateTime.set(Calendar.MONTH, mMonth)
                newDateTime.set(Calendar.DAY_OF_MONTH, mDay)
                new_date.text = DateFormat.getDateInstance().format(newDateTime.time)
            }}, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH),
            currentDateTime.get(Calendar.DAY_OF_MONTH))
        dialog.show()
    }

    fun sendTime(view: View){


    }

    fun setSysTime(view: View){

    }
}
