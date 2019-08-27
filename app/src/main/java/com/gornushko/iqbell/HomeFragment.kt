package com.gornushko.iqbell


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import java.text.DateFormat
import java.util.*
import kotlin.experimental.and


private val iqTime: Calendar = GregorianCalendar.getInstance()
private val df = DateFormat.getDateInstance(DateFormat.LONG)
private val tf = DateFormat.getTimeInstance(DateFormat.DEFAULT)
private lateinit var byteData: ByteArray
private lateinit var extraByteData: ByteArray

@ExperimentalUnsignedTypes
class HomeFragment : Fragment() {

    private lateinit var listener: MyFragmentListener

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MyFragmentListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        view.assembly_button.onClick { listener.sendData(byteArrayOf(0xA)) }
        view.workshop_button.onClick { listener.sendData(byteArrayOf(0x9)) }
        view.ring_button.onClick { listener.sendData(byteArrayOf(0x8)) }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        super.onResume()
        updateView()
    }

    fun setStartData(data: ByteArray){
        byteData = data
    }

    fun setStartExtraData(data: ByteArray){
        extraByteData = data
    }

    @SuppressLint("SetTextI18n")
    private fun updateView(){
        iqTime.timeInMillis = (getLongFromByteArray(byteData.copyOfRange(0, 4)) - 10_800)*1_000 //-3 h (Arduino stores MSC time, Android - UTC)
        date.text = df.format(iqTime.time)
        time.text = tf.format(iqTime.time)
        temperature.text = "${byteData[5].toInt()}" + "℃"
        val iqMode = (byteData[4] and 0x7F).toInt()
        mode.text = getString(when(iqMode){
            0 -> R.string.classes
            1 -> R.string.non_school_day
            2 -> R.string.not_started
            3 ->R.string.finished
            4 -> R.string.charging
            else -> R.string.charged
        })
        val shortTimetable = byteData[4].toUByte().toInt() > 127
        val nextBellByte = byteData[8].toUByte().toInt()
        when(iqMode){
            0, 2, 3 -> {
                timetable.visibility = View.VISIBLE
                timetable.text = getString(if(shortTimetable) R.string.short_day else R.string.normal_classes)
            }
            else -> {
                timetable.visibility = View.GONE
            }
        }
        if(nextBellByte != 255 && iqMode == 0){
            next_bell.visibility = View.VISIBLE
            time_till_next_bell.visibility = View.VISIBLE
            val nextBellNum = nextBellByte + if(shortTimetable) 16 else 0
            val nextBell = extraByteData[nextBellNum].toUByte().toInt()
            val nextBellHour = nextBell/12+8
            val nextBellMinute = nextBell%12*5
            next_bell.text = getString(R.string.next_bell) + ": \n ${print2digits(nextBellHour)}:${print2digits(nextBellMinute)}"
            var secondsTillNextBell = (nextBellHour*3600 + nextBellMinute*60) - (iqTime.get(Calendar.HOUR_OF_DAY)*3600 + iqTime.get(Calendar.MINUTE)*60 + iqTime.get(Calendar.SECOND))
            if(secondsTillNextBell < 0) secondsTillNextBell = 0
            time_till_next_bell.text = getString(R.string.time_till_next_bell) + ": \n ${print2digits(secondsTillNextBell/60)}:${print2digits(secondsTillNextBell%60)}"
        } else{
            next_bell.visibility = View.GONE
            time_till_next_bell.visibility = View.GONE
        }
        ringing_state.text = when(byteData[6].toInt()){
            0 -> getString(R.string.not_ringing)
            1 -> getString(R.string.lesson_ringing) + ": ${byteData[7].toInt()}"
            2 -> getString(R.string.workshop_ringing) + ": ${byteData[7].toInt()}"
            else -> getString(R.string.assembly_ringing) + ": ${byteData[7].toInt()}"
        }
        if(byteData[6].toInt() > 0 || iqMode > 0){
            workshop_button.visibility = View.GONE
            assembly_button.visibility = View.GONE
            ring_button.visibility = View.GONE
            info.text = getString(R.string.manual_bells_control_na)
        } else{
            workshop_button.visibility = View.VISIBLE
            assembly_button.visibility = View.VISIBLE
            ring_button.visibility = View.VISIBLE
            info.text = getString(R.string.manual_bells_control)
        }
    }

    private fun print2digits(num: Int) = if(num < 10) "0$num" else "$num"

    fun updateData(data: ByteArray){
        byteData = data
        updateView()
    }

    fun updateExtraData(data: ByteArray){
        extraByteData = data
        updateView()
    }

    private fun getLongFromByteArray(data: ByteArray): Long{
        var result = 0u
        for(i in 3 downTo 0) result = (result shl 8) + data[i].toUByte()
        return result.toLong()
    }
}
