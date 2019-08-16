package com.gornushko.iqbell


import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.fragment.app.ListFragment

@ExperimentalUnsignedTypes
abstract class MyListFragment : ListFragment() {

    abstract var stringData: Array<String>
    lateinit var byteData: ByteArray
    var adapter: MyArrayAdapter? = null
    var listener: MyFragmentListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        adapter = MyArrayAdapter(activity!!, android.R.layout.simple_list_item_1, stringData)
        listener = context as MyFragmentListener
    }

    fun setStartData(data: ByteArray){
        byteData = data
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        listAdapter = adapter
        updateView()
    }

    fun updateData(data: ByteArray){
        byteData = data
        updateView()
    }


    abstract fun updateView()

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        adapter?.selectedItem = if(position == adapter?.selectedItem) null else position
        if(adapter?.selectedItem == null) listener?.noEdit()
        else listener?.edit()
        adapter?.notifyDataSetChanged()
    }


    fun resetSelectedState(){
        adapter?.selectedItem = null
        adapter?.notifyDataSetChanged()
    }

    abstract fun clear()

    abstract fun edit()

    fun send() = byteData
}