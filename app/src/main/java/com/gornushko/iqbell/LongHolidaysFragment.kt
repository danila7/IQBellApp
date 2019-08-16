package com.gornushko.iqbell

import kotlin.experimental.or

@ExperimentalUnsignedTypes
class LongHolidaysFragment : MyListFragment(){

    override var stringData = Array(8) {" "}

    override fun updateView() {
/*
        for(i in 0..7){
            val t = byteData[i]
            if(t.toUByte() > 127.toUByte()) stringData[i] = "${i+1}:"
            else stringData[i] = "${i+1}:   "
        }*/
        adapter?.notifyDataSetChanged()
    }

    override fun clear(){
        byteData[adapter?.selectedItem!!] = byteData[adapter?.selectedItem!!] or  0x80.toUByte().toByte()
        updateView()
    }

    override fun edit(){
        //val tt = (byteData[adapter?.selectedItem!!].toUByte() and 0x7F.toUByte()).toInt()

    }

}