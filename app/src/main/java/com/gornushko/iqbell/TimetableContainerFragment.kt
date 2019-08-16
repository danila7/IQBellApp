package com.gornushko.iqbell

import android.content.Context


@ExperimentalUnsignedTypes
class TimetableContainerFragment : MyContainerFragment() {
    override val one = TimetableFragment()
    override val two = TimetableFragment()
    override var nameFirst = String()
    override var nameSecond = String()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        nameFirst = getString(R.string.timetables_main_tab)
        nameSecond = getString(R.string.timetables_second_tab)
    }

    override fun setStartData(data: ByteArray){
        one.setStartData(data.copyOfRange(0, 16))
        two.setStartData(data.copyOfRange(16, 32))
    }

    override fun updateData(data: ByteArray){
        one.updateData(data.copyOfRange(0, 16))
        two.updateData(data.copyOfRange(16, 32))
    }

    override fun send(){
        val dataToSend = if(activeTab == 0) ByteArray(1){0x1} + one.send()
        else ByteArray(1){0x6} + two.send()
        listener?.sendData(dataToSend, true)
    }

}
