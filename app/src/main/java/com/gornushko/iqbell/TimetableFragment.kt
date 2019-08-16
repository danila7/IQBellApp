package com.gornushko.iqbell

import android.app.TimePickerDialog
import kotlin.experimental.or

@ExperimentalUnsignedTypes
class TimetableFragment : MyListFragment() {

    override var stringData = Array(16) {" "}


    override fun updateView(){
        for(i in 0..15){
            val t = byteData[i]
            if(t.toUByte() > 127.toUByte()) stringData[i] = "${i+1}."
            else stringData[i] = "${i+1}.   ${print2digits(t/12+8)}:${print2digits(t%12*5)}"
        }
        adapter?.notifyDataSetChanged()
    }

    private fun print2digits(num: Int) = if(num < 10) "0$num" else "$num"

    override fun clear(){
        byteData[adapter?.selectedItem!!] = byteData[adapter?.selectedItem!!] or  0x80.toUByte().toByte()
        updateView()
    }

    override fun edit(){
        val tt = (byteData[adapter?.selectedItem!!].toUByte() and 0x7F.toUByte()).toInt()
        TimePickerDialog(activity!!, android.R.style.ThemeOverlay_Material_Dialog,
            TimePickerDialog.OnTimeSetListener{ _, mHour, mMinute -> run{
                val t = (((mHour-8)*12)+(mMinute/5)).toUByte()
                if(t < 128.toUByte()) {
                    byteData[adapter?.selectedItem!!] = t.toByte()
                    updateView()
                }
            }}, tt/12+8, tt%12*5, true).show()
    }
}
