package com.gornushko.iqbell

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.fragment.app.ListFragment

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
        updateData(byteData)
    }

    fun updateData(data: ByteArray){
        for(i in 0..15){
            stringData[i] = "${i+1}:   ${print2digits(data[i]/12+8)}:${print2digits(data[i]%12*5)}"
        }
        adapter = MyArrayAdapter(activity!!, android.R.layout.simple_list_item_1, stringData)
        listAdapter = adapter
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
        stringData[adapter.selectedItem!!] = "${adapter.selectedItem!!+1}: "
        adapter.notifyDataSetChanged()
    }

    fun edit(){
        val dialog = TimePickerDialog(activity!!, android.R.style.ThemeOverlay_Material_Dialog,
            TimePickerDialog.OnTimeSetListener{ _, mHour, mMinute -> run{
                stringData[adapter.selectedItem!!] = "${adapter.selectedItem!!+1}:   ${print2digits(mHour)}:${print2digits(mMinute)}"
                adapter.notifyDataSetChanged()
            }}, byteData[adapter.selectedItem!!]/12+8, byteData[adapter.selectedItem!!]%12*5, true)
        dialog.show()
    }
}
