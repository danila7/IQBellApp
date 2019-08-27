package com.gornushko.iqbell

import android.app.DatePickerDialog
import org.jetbrains.anko.okButton
import org.jetbrains.anko.support.v4.alert
import java.util.*

@ExperimentalUnsignedTypes
class LongHolidaysFragment : MyListFragment(){

    override var stringData = Array(8) {" "}

    override fun updateView() {
        if(activity != null){
            for(i in 0..7) {
                var invalid = false
                val startDate = GregorianCalendar()
                val endDate = GregorianCalendar()
                val startMonth= byteData[i*4].toUByte().toInt()
                val startDay = byteData[i*4+1].toUByte().toInt()
                val endMonth = byteData[i*4+2].toUByte().toInt()
                val endDay = byteData[i*4+3].toUByte().toInt()
                if(startMonth == 0 || startMonth > 12) invalid = true
                if(startDay == 0 || startDay > 31) invalid = true
                if(endMonth == 0 || endMonth > 12) invalid = true
                if(endDay == 0 || endDay > 31) invalid = true
                if(invalid){
                    stringData[i] = "${i + 1}."
                } else{

                    startDate.set(2000, startMonth - 1, startDay)
                    endDate.set(2000, endMonth - 1, endDay)
                    stringData[i] = "${i + 1}.   " + getString(R.string.from) + String.format(" %1\$te %1\$tB ", startDate) +
                            getString(R.string.to) + String.format(" %1\$te %1\$tB", endDate)
                }
            }
            adapter?.notifyDataSetChanged()
        }
    }

    override fun clear(){
        for (i in 0..3) byteData[adapter?.selectedItem!!*4+i] = 0xFF.toByte()
        updateView()
    }

    override fun edit(){
        val c = GregorianCalendar()
        alert(getString(R.string.set_start_date)) {
            okButton {
                DatePickerDialog(activity!!, R.style.ThemeOverlay_AppCompat_Dialog,
                    DatePickerDialog.OnDateSetListener{ _, _, mMonthStart, mDayStart -> run{
                        alert(getString(R.string.set_end_date)) {
                            okButton {
                                DatePickerDialog(activity!!, R.style.ThemeOverlay_AppCompat_Dialog,
                                    DatePickerDialog.OnDateSetListener{ _, _, mMonthEnd, mDayEnd -> run{
                                        byteData[adapter?.selectedItem!!*4] = (mMonthStart+1).toUByte().toByte()
                                        byteData[adapter?.selectedItem!!*4+1] = mDayStart.toUByte().toByte()
                                        byteData[adapter?.selectedItem!!*4+2] = (mMonthEnd+1).toUByte().toByte()
                                        byteData[adapter?.selectedItem!!*4+3] = mDayEnd.toUByte().toByte()
                                        updateView()
                                    }}, c.get(Calendar.YEAR), 7, 31).show()
                            }
                        }.show()
                    }}, c.get(Calendar.YEAR), 4,31).show()
            }
        }.show()
    }

}