package com.gornushko.iqbell


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import kotlinx.android.synthetic.main.fragment_settings.*
import org.jetbrains.anko.support.v4.toast


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
@ExperimentalUnsignedTypes
class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        autoAuth.isChecked = activity!!.getSharedPreferences(MainActivity.SH_PREFS, Context.MODE_PRIVATE).getString(IQService.AUTO_AUTH, "0") == "1"
        autoAuth.setOnClickListener {
            val ed = activity!!.getSharedPreferences(MainActivity.SH_PREFS, Context.MODE_PRIVATE).edit()
            ed.putString(IQService.AUTO_AUTH, if(autoAuth.isChecked) "1" else "0")
            ed.apply()
        }
    }


}
