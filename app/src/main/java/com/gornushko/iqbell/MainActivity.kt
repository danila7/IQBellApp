package com.gornushko.iqbell

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity(), MyFragmentListener {

    companion object Const{
        const val START_DATA = "start_d"
        const val START_EXTRA_DATA = "start_x"
    }

    private var goingBack = false
    private lateinit var dialog: AlertDialog
    private lateinit var lastData: ByteArray
    private val homeFragment = HomeFragment()
    private val timetableContainerFragment = TimetableContainerFragment()
    private val holidaysContainerFragment = HolidaysContainerFragment()
    private val batteryFragment = BatteryFragment()
    private val timeFragment = TimeFragment()
    private var active: Fragment = homeFragment
    private val fm = supportFragmentManager
    private var edit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar?)
        bottomNavigation.setOnNavigationItemSelectedListener(navListener)
        fm.beginTransaction().add(R.id.fragment_container, timeFragment, "5").hide(timeFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, batteryFragment, "4").hide(batteryFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, holidaysContainerFragment, "3").hide(holidaysContainerFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, timetableContainerFragment, "2").hide(timetableContainerFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit()
        val startData = intent.getByteArrayExtra(START_DATA)!!
        val startExtraData  = intent.getByteArrayExtra(START_EXTRA_DATA)!!
        homeFragment.setStartData(startData.copyOfRange(0, 4))
        timeFragment.setStartData(startData.copyOfRange(0, 4))
        batteryFragment.setStartData(startData[4])
        timetableContainerFragment.setStartData(startExtraData.copyOfRange(0, 32))
        holidaysContainerFragment.setStartData(startExtraData.copyOfRange(32, 80))
        startService(intentFor<IQService>(IQService.ACTION to IQService.NEW_PENDING_INTENT, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when(active){
            is HomeFragment -> menuInflater.inflate(R.menu.menu_toolbar, menu)
            is TimetableContainerFragment -> menuInflater.inflate(if(edit)R.menu.menu_toolbar_send_edit else R.menu.menu_toolbar_send, menu)
            is HolidaysContainerFragment -> menuInflater.inflate(if(edit)R.menu.menu_toolbar_send_edit else R.menu.menu_toolbar_send, menu)
            is BatteryFragment -> menuInflater.inflate(R.menu.menu_toolbar, menu)
            is TimeFragment -> menuInflater.inflate(R.menu.menu_toolbar_send, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_about -> startActivity(intentFor<AboutActivity>())
            R.id.action_send -> when(active){
                is TimeFragment -> timeFragment.send()
                is TimetableContainerFragment -> timetableContainerFragment.send()
                is HolidaysContainerFragment -> holidaysContainerFragment.send()
            }
            R.id.action_edit -> when(active){
                is TimetableContainerFragment -> timetableContainerFragment.edit()
                is HolidaysContainerFragment -> holidaysContainerFragment.edit()
            }
            R.id.action_clear -> when(active){
                is TimetableContainerFragment -> timetableContainerFragment.clear()
                is HolidaysContainerFragment -> holidaysContainerFragment.clear()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.action_home -> {
                fm.beginTransaction().hide(active).show(homeFragment).commit()
                active = homeFragment
            }
            R.id.action_time -> {
                fm.beginTransaction().hide(active).show(timeFragment).commit()
                active = timeFragment
            }
            R.id.action_battery -> {
                fm.beginTransaction().hide(active).show(batteryFragment).commit()
                active = batteryFragment
            }
            R.id.action_holidays -> {
                fm.beginTransaction().hide(active).show(holidaysContainerFragment).commit()
                active = holidaysContainerFragment
            }
            R.id.action_timetable -> {
                fm.beginTransaction().hide(active).show(timetableContainerFragment).commit()
                active = timetableContainerFragment
            }
        }
        timetableContainerFragment.resetSelectedState()
        holidaysContainerFragment.resetSelectedState()
        noEdit()
        invalidateOptionsMenu()
        return@OnNavigationItemSelectedListener true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(resultCode){
            IQService.DEVICE_STATE -> {
                val state = data!!.getByteArrayExtra(IQService.DATA)!!
                homeFragment.updateData(state.copyOfRange(0, 4))
                timeFragment.updateData(state.copyOfRange(0, 4))
                batteryFragment.updateData(state[4])
            }
            IQService.RECONNECTING, IQService.BT_OFF -> {
                goingBack = true
                startActivity(intentFor<ConnectActivity>(ConnectActivity.KEY to resultCode).newTask().clearTask().clearTop())
            }
            IQService.NEW_EXTRA -> {
                val extra = data!!.getByteArrayExtra(IQService.EXTRA_DATA)
                if(extra == null){
                    alert(R.string.error_updating_extra) {
                        title = getString(R.string.error)
                        positiveButton(getString(R.string.repeat)){startService(intentFor<IQService>(IQService.ACTION to IQService.GET_EXTRA_DATA))}
                        negativeButton(getString(R.string.cancel)){}
                    }.show()
                } else{
                    timetableContainerFragment.updateData(extra.copyOfRange(0, 32))
                    holidaysContainerFragment.updateData(extra.copyOfRange(32, 80))

                    alert(R.string.data_updated) {
                        okButton {}
                    }.show()
                }
            }
            IQService.ERROR -> {
                dialog.dismiss()
                alert (R.string.error_sending){
                    title = getString(R.string.error)
                    positiveButton(getString(R.string.repeat)){sendData(lastData)}
                    negativeButton(getString(R.string.cancel)){}
                }.show()
            }
            IQService.OK -> {
                dialog.dismiss()
                alert(R.string.data_was_sent) {
                    okButton {}
                }.show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!goingBack) startService(intentFor<IQService>(IQService.ACTION to IQService.STOP_SERVICE))
    }

    override fun sendData(data: ByteArray, updateExtra: Boolean){
        startService(intentFor<IQService>(IQService.ACTION to IQService.SEND_DATA, IQService.DATA to data, IQService.GET_EXTRA_DATA to updateExtra))
        lastData = data
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.layout_loading_dialog)
        dialog = builder.create()
        dialog.show()
    }


    override fun noEdit() {
        edit = false
        invalidateOptionsMenu()
    }

    override fun edit() {
        edit = true
        invalidateOptionsMenu()
    }
}
