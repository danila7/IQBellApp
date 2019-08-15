package com.gornushko.iqbell


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_timetable_container.view.*

class TimetableContainerFragment : Fragment() {

    private val main = TimetableFragment()
    private val second = TimetableFragment()
    lateinit var listener: MyFragmentListener
    private var activeTimetable = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MyFragmentListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_timetable_container, container, false)
        view.pager.adapter = Adapter()
        view.pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                activeTimetable = position
                listener.noEdit()
                if(position == 1) main.resetSelectedState()
                else second.resetSelectedState()
            }
        })
        return view
    }


    fun setStartData(data: ByteArray){
        main.setStartData(data.copyOfRange(0, 16))
        second.setStartData(data.copyOfRange(16, 32))
    }

    fun updateData(data: ByteArray){
        main.updateData(data.copyOfRange(0, 16))
        second.updateData(data.copyOfRange(16, 32))
    }

    fun clear(){
        if(activeTimetable == 0) main.clear()
        else second.clear()
    }

    fun edit(){
        if(activeTimetable == 0) main.edit()
        else second.edit()
    }

    inner class Adapter : FragmentPagerAdapter(childFragmentManager){

        override fun getItem(position: Int) = if(position == 0) main else second

        override fun getCount() = 2

        override fun getPageTitle(position: Int) = getText(if(position == 0) R.string.main_tab else R.string.second_tab)
    }
}
