package com.gornushko.iqbell

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.fragment.app.ListFragment
import kotlin.experimental.or

@ExperimentalUnsignedTypes
class TimetableFragment : ListFragment() {

    private lateinit var byteData: ByteArray
    private lateinit var adapter: MyArrayAdapter
    private lateinit var listener: MyFragmentListener
    private var stringData = Array(16) {" "}

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MyFragmentListener
    }

    fun setStartData(data: ByteArray){
        byteData = data
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter = MyArrayAdapter(activity!!, android.R.layout.simple_list_item_1, stringData)
        listAdapter = adapter
        updateView()
    }

    fun updateData(data: ByteArray){
        byteData = data
        updateView()
    }

    private fun updateView(){
        for(i in 0..15){
            val t = byteData[i]
            if(t.toUByte() > 127.toUByte()) stringData[i] = "${i+1}:"
            else stringData[i] = "${i+1}:   ${print2digits(t/12+8)}:${print2digits(t%12*5)}"
        }
        adapter.notifyDataSetChanged()
    }

    private fun print2digits(num: Int) = if(num < 10) "0$num" else "$num"

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        adapter.selectedItem = if(position == adapter.selectedItem) null else position
        if(adapter.selectedItem == null) listener.noEdit()
        else listener.edit()
        adapter.notifyDataSetChanged()
    }

    fun resetSelectedState(){
        adapter.selectedItem = null
        adapter.notifyDataSetChanged()
    }

    fun clear(){
        byteData[adapter.selectedItem!!] = byteData[adapter.selectedItem!!] or  0x80.toUByte().toByte()
        updateView()
    }

    fun edit(){
        val tt = (byteData[adapter.selectedItem!!].toUByte() and 0x7F.toUByte()).toInt()
        val dialog = TimePickerDialog(activity!!, android.R.style.ThemeOverlay_Material_Dialog,
            TimePickerDialog.OnTimeSetListener{ _, mHour, mMinute -> run{
                val t = (((mHour-8)*12)+(mMinute/5)).toUByte()
                if(t < 128.toUByte()) {
                    byteData[adapter.selectedItem!!] = t.toByte()
                    updateView()
                }
            }}, tt/12+8, tt%12*5, true)
        dialog.show()
    }

    fun send() = byteData
}
