package com.gornushko.iqbell

import android.app.DatePickerDialog
import org.jetbrains.anko.support.v4.alert
import java.util.*

@ExperimentalUnsignedTypes
class ShortHolidaysFragment : MyListFragment(){

    override var stringData = Array(8) {" "}

    override fun updateView() {
        for(i in 0..7){
            var invalid = false
            val c = GregorianCalendar()
            var t = byteData[i*2].toUByte()
            val isShort = (t > 127.toUInt())
            t = t and 0x7F.toUByte()
            val month = t.toInt()
            if(month == 0 || month > 12) invalid = true
            t = byteData[i*2+1].toUByte()
            val day = t.toInt()
            if(day == 0 || day > 31) invalid = true
            if(invalid){
                stringData[i] = "$i."
            } else{
                c.set(2000, month - 1, day)
                stringData[i] = "$i.   " + String.format("%1\$te %1\$tB : ", c) +
                        getString(if(isShort)R.string.short_day else R.string.holiday_day)
            }
        }
        adapter?.notifyDataSetChanged()
    }

    override fun clear(){
        for(i in 0..1) byteData[adapter?.selectedItem!!*2+i] = 0xFF.toByte()
        updateView()
    }

    override fun edit(){
        val c = GregorianCalendar()
        DatePickerDialog(activity!!, android.R.style.ThemeOverlay_Material_Dialog,
            DatePickerDialog.OnDateSetListener{ _, _, mMonth, mDay -> run{
                alert(getString(R.string.choose_short_or_holiday)) {
                    negativeButton(getString((R.string.holiday_day))){
                        byteData[adapter?.selectedItem!!*2] = (mMonth+1).toUByte().toByte()
                        byteData[adapter?.selectedItem!!*2+1] = mDay.toUByte().toByte()
                        updateView()
                    }
                    positiveButton(getString(R.string.short_day)){
                        byteData[adapter?.selectedItem!!*2] = ((mMonth+1).toUByte() or 0x80.toUByte()).toByte()
                        byteData[adapter?.selectedItem!!*2+1] = mDay.toUByte().toByte()
                        updateView()
                    }
                }.show()
            }}, c.get(Calendar.YEAR), c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)).show()
    }
}