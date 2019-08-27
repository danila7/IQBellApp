package com.gornushko.iqbell


import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_time.*
import java.text.DateFormat
import java.util.*
import kotlinx.android.synthetic.main.fragment_time.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick


private val currentDateTime: Calendar = GregorianCalendar.getInstance()
private val newDateTime: Calendar = GregorianCalendar.getInstance()
private val df = DateFormat.getDateInstance(DateFormat.LONG)
private val tf = DateFormat.getTimeInstance(DateFormat.DEFAULT)
private lateinit var startData: ByteArray

@ExperimentalUnsignedTypes
class TimeFragment : Fragment() {

    private lateinit var listener: MyFragmentListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MyFragmentListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_time, container, false)
        view.set_time.onClick { pickTime() }
        view.set_date.onClick { pickDate() }
        view.set_android_td.onClick {
            newDateTime.timeInMillis = System.currentTimeMillis()
            new_date.text = df.format(newDateTime.time)
            new_time.text = tf.format(Date())
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        super.onResume()
        updateData(startData)
    }

    fun setStartData(data: ByteArray){
        startData = data
    }

    fun updateData(timeData: ByteArray){
        currentDateTime.timeInMillis = (getLongFromByteArray(timeData) - 10_800)*1_000 //-3 h (Arduino stores MSC time, Android - UTC)
        current_time.text = tf.format(currentDateTime.time)
        current_date.text = df.format(currentDateTime.time)
    }

    private fun getLongFromByteArray(data: ByteArray): Long{
        var result = 0u
        for(i in 3 downTo 0) result = (result shl 8) + data[i].toUByte()
        return result.toLong()
    }

    private fun pickTime(){
        TimePickerDialog(activity!!, R.style.ThemeOverlay_AppCompat_Dialog,
            TimePickerDialog.OnTimeSetListener{ _, mHour, mMinute -> run{
                newDateTime.set(Calendar.HOUR_OF_DAY, mHour)
                newDateTime.set(Calendar.MINUTE, mMinute)
                newDateTime.set(Calendar.SECOND, 0)
                new_time.text = tf.format(newDateTime.time)
            }}, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true).show()
    }

    private fun pickDate(){
        DatePickerDialog(activity!!, R.style.ThemeOverlay_AppCompat_Dialog,
            DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay -> run{
                newDateTime.set(Calendar.YEAR, mYear)
                newDateTime.set(Calendar.MONTH, mMonth)
                newDateTime.set(Calendar.DAY_OF_MONTH, mDay)
                new_date.text = df.format(newDateTime.time)
            }}, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH),
            currentDateTime.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun makeByteArrayFromLong(sum: Long): ByteArray{
        val result = ByteArray(4)
        val tempSum = sum.toUInt()
        for(i in 0..3) result[i] = (tempSum shr 8*i).toUByte().toByte()
        return result
    }

    fun send(){
        val timeForArduino = newDateTime.timeInMillis / 1_000 + 10_800
        val dataToSend = ByteArray(1){0x4} + makeByteArrayFromLong(timeForArduino)
        listener.sendData(dataToSend)
    }
}
