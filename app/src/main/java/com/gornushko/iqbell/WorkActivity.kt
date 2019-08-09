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
    private val df = DateFormat.getDateInstance(DateFormat.LONG)
    private val tf = DateFormat.getTimeInstance(DateFormat.DEFAULT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)
        startService(intentFor<IQService>(IQService.ACTION to IQService.NEW_PENDING_INTENT, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Yeah! request: $requestCode result: $resultCode")
        when(resultCode){
            IQService.GET_INFO_RESULT -> {
                getDeviceInfo(data!!.getByteArrayExtra(IQService.DATA)!!)

            }
            IQService.RECONNECTING, IQService.BT_OFF -> {
                goingBack = true
                startActivity(intentFor<MainActivity>(MainActivity.KEY to resultCode).newTask().clearTask().clearTop())
            }
        }
    }

    fun getInfo(view: View){
        startService(intentFor<IQService>(IQService.ACTION to IQService.DATA_TRANSFER, IQService.TASK to 0, IQService.DATA to ByteArray(1)))
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!goingBack) startService(intentFor<IQService>(IQService.ACTION to IQService.STOP_SERVICE))
    }

    private fun getDeviceInfo(data: ByteArray){
        currentDateTime.timeInMillis = (getLongFromByteArray(data) - 10_800)*1_000 //-3 h (Arduino stores MSC time, Android - UTC)
        current_time.text = tf.format(currentDateTime.time)
        current_date.text = df.format(currentDateTime.time)

    }

    fun timePicker(view: View){
        val dialog = TimePickerDialog(this, android.R.style.ThemeOverlay_Material_Dialog,
            TimePickerDialog.OnTimeSetListener{_, mHour, mMinute -> run{
                newDateTime.set(Calendar.HOUR_OF_DAY, mHour)
                newDateTime.set(Calendar.MINUTE, mMinute)
                newDateTime.set(Calendar.SECOND, 0)
                new_time.text = tf.format(newDateTime.time)
            }}, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true)
        dialog.show()
    }

    fun datePicker(view: View){
        val dialog = DatePickerDialog(this, android.R.style.ThemeOverlay_Material_Dialog,
            DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay -> run{
                newDateTime.set(Calendar.YEAR, mYear)
                newDateTime.set(Calendar.MONTH, mMonth)
                newDateTime.set(Calendar.DAY_OF_MONTH, mDay)
                new_date.text = df.format(newDateTime.time)
            }}, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH),
            currentDateTime.get(Calendar.DAY_OF_MONTH))
        dialog.show()
    }

    fun sendTime(view: View){
        val timeForArduino = newDateTime.timeInMillis / 1_000 + 10_800
        val data = ByteArray(1){0x4} + makeByteArrayFromLong(timeForArduino)
        startService(intentFor<IQService>(IQService.ACTION to IQService.DATA_TRANSFER, IQService.TASK to 4, IQService.DATA to data))
    }

    fun setSysTime(view: View){
        newDateTime.timeInMillis = System.currentTimeMillis()
        new_date.text = df.format(newDateTime.time)
        new_time.text = tf.format(Date())
    }

    private fun getLongFromByteArray(data: ByteArray): Long{
        var result = 0u
        for(i in 3 downTo 0) result = (result shl 8) + data[i].toUByte()
        return result.toLong()
    }

    private fun makeByteArrayFromLong(sum: Long): ByteArray{
        val result = ByteArray(4)
        val tempSum = sum.toUInt()
        for(i in 0..3) result[i] = (tempSum shr 8*i).toUByte().toByte()
        return result
    }
}
