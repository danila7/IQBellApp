package com.gornushko.iqbell


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_container.view.*

@ExperimentalUnsignedTypes
abstract class MyContainerFragment : Fragment() {

    abstract val one: MyListFragment
    abstract val two: MyListFragment
    abstract var nameFirst: String
    abstract var nameSecond: String
    var listener: MyFragmentListener? = null
    var activeTab = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as MyFragmentListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_container, container, false)
        view.pager.adapter = Adapter()
        view.pager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                activeTab = position
                listener?.noEdit()
                if(position == 1) one.resetSelectedState()
                else two.resetSelectedState()
            }
        })
        return view
    }

    fun resetSelectedState(){
        one.resetSelectedState()
        two.resetSelectedState()
    }


    abstract fun setStartData(data: ByteArray)

    abstract fun updateData(data: ByteArray)

    fun clear(){
        if(activeTab == 0) one.clear()
        else two.clear()
    }

    fun edit(){
        if(activeTab == 0) one.edit()
        else two.edit()
    }

    abstract fun send()

    inner class Adapter : FragmentPagerAdapter(childFragmentManager){

        override fun getItem(position: Int) = if(position == 0) one else two

        override fun getCount() = 2

        override fun getPageTitle(position: Int) = if(position == 0) nameFirst else nameSecond
    }

}
