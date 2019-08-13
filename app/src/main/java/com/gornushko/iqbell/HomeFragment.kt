package com.gornushko.iqbell


import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.fragment_home.*
import java.text.DateFormat
import java.util.*


private val dateTime: Calendar = GregorianCalendar.getInstance()
private val dtf = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.DEFAULT)
private val df = DateFormat.getDateInstance(DateFormat.LONG)
private val tf = DateFormat.getTimeInstance(DateFormat.DEFAULT)
private lateinit var startData: ByteArray

@ExperimentalUnsignedTypes
class HomeFragment : Fragment() {

    companion object Const{
        private const val TAG = "HomeFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView")
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        super.onResume()
        updateTime(startData)
    }

    fun setStartData(data: ByteArray){
        startData = data
    }

    fun updateTime(timeData: ByteArray){
        dateTime.timeInMillis = (getLongFromByteArray(timeData) - 10_800)*1_000 //-3 h (Arduino stores MSC time, Android - UTC)
        time.text = dtf.format(dateTime.time)
        clock.setTime(dateTime.get(Calendar.HOUR), dateTime.get(Calendar.MINUTE), dateTime.get(Calendar.SECOND))

    }

    private fun getLongFromByteArray(data: ByteArray): Long{
        var result = 0u
        for(i in 3 downTo 0) result = (result shl 8) + data[i].toUByte()
        return result.toLong()
    }
}
