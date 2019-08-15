package com.gornushko.iqbell

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    companion object Const{
        private const val TAG = "Bluetooth Action"
        const val START_DATA = "start_d"
        const val START_EXTRA_DATA = "start_x"

    }

    private var goingBack = false

    private val homeFragment = HomeFragment()
    private val timetableFragment = TimetableFragment()
    private val calendarFragment = CalendarFragment()
    private val batteryFragment = BatteryFragment()
    private val timeFragment = TimeFragment()
    private var active: Fragment = homeFragment
    private val fm = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar?)
        bottomNavigation.setOnNavigationItemSelectedListener(navListener)
        fm.beginTransaction().add(R.id.fragment_container, timeFragment, "5").hide(timeFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, batteryFragment, "4").hide(batteryFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, calendarFragment, "3").hide(calendarFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, timetableFragment, "2").hide(timetableFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit()
        val startData = intent.getByteArrayExtra(START_DATA)!!
        val startExtraData  = intent.getByteArrayExtra(START_EXTRA_DATA)!!
        homeFragment.setStartData(startData.copyOfRange(0, 4))
        timeFragment.setStartData(startData.copyOfRange(0, 4))
        batteryFragment.setStartData(startData[4])
        timetableFragment.setStartData(startExtraData.copyOfRange(0, 32))
        startService(intentFor<IQService>(IQService.ACTION to IQService.NEW_PENDING_INTENT, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        when(active){
            is HomeFragment -> menuInflater.inflate(R.menu.menu_toolbar, menu)
            is TimetableFragment -> menuInflater.inflate(R.menu.menu_toolbar, menu)
            is CalendarFragment -> menuInflater.inflate(R.menu.menu_toolbar, menu)
            is BatteryFragment -> menuInflater.inflate(R.menu.menu_toolbar, menu)
            is TimeFragment -> menuInflater.inflate(R.menu.nenu_toolbar_send, menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_about -> startActivity(intentFor<AboutActivity>())
            R.id.action_send -> when(active){
                is TimeFragment -> timeFragment.send()
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
            R.id.action_calendar -> {
                fm.beginTransaction().hide(active).show(calendarFragment).commit()
                active = calendarFragment
            }
            R.id.action_timetable -> {
                fm.beginTransaction().hide(active).show(timetableFragment).commit()
                active = timetableFragment
            }
        }

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
                    toast("Error updating extra info")
                } else{
                    timetableFragment.updateData(extra)
                }
            }
            IQService.BUSY -> toast(getString(R.string.busy))
            IQService.ERROR -> toast(R.string.error_sendind)
            IQService.OK -> toast(R.string.data_was_sent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!goingBack) startService(intentFor<IQService>(IQService.ACTION to IQService.STOP_SERVICE))
    }
}
