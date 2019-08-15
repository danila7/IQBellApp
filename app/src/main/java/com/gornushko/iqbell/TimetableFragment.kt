package com.gornushko.iqbell


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_timetable.*
import kotlinx.android.synthetic.main.fragment_timetable.view.*


private lateinit var startData: ByteArray
private val main = MainTimetableFragment()
private val second = SecondTimetableFragment()


class TimetableFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_timetable, container, false)
        view.pager.adapter = Adapter()
        return view
    }

    fun setStartData(data: ByteArray){
        startData = data
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateData(startData)
    }

    fun updateData(data: ByteArray){
        val stringData = Array(16) {" "}
        for(i in 0..15){
            stringData[i] = "${i+1}:   ${print2digits(data[i]/12+8)}:${print2digits(data[i]%12*5)}"
        }
        val adapter = ArrayAdapter(activity!!, android.R.layout.simple_list_item_1, stringData)
        //listAdapter = adapter
    }

    private fun print2digits(num: Int) = if(num < 10) "0$num" else "$num"


    inner class Adapter : FragmentPagerAdapter(childFragmentManager){

        override fun getItem(position: Int) = if(position == 0) main else second

        override fun getCount() = 2

        override fun getPageTitle(position: Int) = getText(if(position == 0) R.string.main_tab else R.string.second_tab)
    }
}
