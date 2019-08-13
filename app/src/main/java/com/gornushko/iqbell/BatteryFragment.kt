package com.gornushko.iqbell


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_battery.*

private var level: Int = 0

class BatteryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_battery, container, false)
    }

    fun setStartData(byteLevel: Byte){
        level = byteLevel.toInt()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateData(level.toByte())
    }

    @SuppressLint("SetTextI18n")
    fun updateData(byteLevel: Byte){
        battery.chargeLevel = byteLevel.toInt()
        batteryLevel.text = "$byteLevel%"
    }

}
