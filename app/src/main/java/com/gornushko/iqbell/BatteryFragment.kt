package com.gornushko.iqbell


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import kotlinx.android.synthetic.main.fragment_battery.*
import kotlinx.android.synthetic.main.fragment_battery.view.*

private var level: Int = 0
private var brightness: Int = 0
private var isCharging: Int = 0

@ExperimentalUnsignedTypes
class BatteryFragment : Fragment() {

    var listener: MyFragmentListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MyFragmentListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_battery, container, false)
        view.change_brigtness.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean){}

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            @SuppressLint("SetTextI18n")
            override fun onStopTrackingTouch(p0: SeekBar?) {
                brightness = view.change_brigtness.progress
                brightness_label.text = getString(R.string.brightness) + ": ${(brightness.toFloat()/2.55).toInt()}%"
                listener?.sendData(byteArrayOf(0x7, brightness.toUByte().toByte()), true)
            }

        })
        return view
    }

    fun setStartData(byteLevel: Byte, charging: Byte){
        isCharging = charging.toInt()
        level = byteLevel.toUByte().toInt()
    }

    fun setStartExtraData(br: Byte){
        brightness = br.toUByte().toInt()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateView()
    }

    @SuppressLint("SetTextI18n")
    fun updateView(){

        when (isCharging){
            0 -> {
                battery.chargeLevel = level
                batteryLevel.text = "$level%, " + getString(R.string.not_charging)
                battery.isCharging = false
            }
            1 -> {
                battery.chargeLevel = level-1
                batteryLevel.text = "${level-1}%, " + getString(R.string.charging)
                battery.isCharging = true
            }
            2 -> {
                battery.chargeLevel = 100
                batteryLevel.text = "100%, " + getString(R.string.charged)
                battery.isCharging = true
            }
        }
        brightness_label.text = getString(R.string.brightness) + ": ${(brightness.toFloat()/2.55).toInt()}%"
        view!!.change_brigtness.progress = brightness
    }

    fun updateData(byteLevel: Byte,  charging: Byte){
        isCharging = charging.toInt()
        level = byteLevel.toUByte().toInt()
        updateView()
    }

    fun updateExtraData(br: Byte){
        brightness = br.toUByte().toInt()
        updateView()
    }

}
