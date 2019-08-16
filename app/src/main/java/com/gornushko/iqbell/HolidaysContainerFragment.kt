package com.gornushko.iqbell

import android.content.Context


@ExperimentalUnsignedTypes
class HolidaysContainerFragment : MyContainerFragment() {
    override val one = ShortHolidaysFragment()
    override val two = LongHolidaysFragment()
    override var nameFirst = String()
    override var nameSecond = String()

    override fun send() {
        val dataToSend = if(activeTab == 0) ByteArray(1){0x3} + one.send()
        else ByteArray(1){0x2} + two.send()
        listener?.sendData(dataToSend, true)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        nameFirst = getString(R.string.holidays_short_tab)
        nameSecond = getString(R.string.holidays_long_tab)
    }

    override fun setStartData(data: ByteArray){
        two.setStartData(data.copyOfRange(0, 32))
        one.setStartData(data.copyOfRange(32, 48))
    }

    override fun updateData(data: ByteArray){
        two.updateData(data.copyOfRange(0, 32))
        one.updateData(data.copyOfRange(32, 48))
    }

}
