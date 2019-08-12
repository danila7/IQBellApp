package com.gornushko.iqbell

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.*
import java.text.DateFormat
import java.util.*
import kotlin.concurrent.timerTask

@ExperimentalUnsignedTypes
class MainActivity : AppCompatActivity() {

    companion object Const{
        private const val TAG = "Bluetooth Action"
        const val SH_PREFS = "shPrefs"
    }

    private var goingBack = false
    private val currentDateTime: Calendar = GregorianCalendar.getInstance()
    private val newDateTime: Calendar = GregorianCalendar.getInstance()
    private val df = DateFormat.getDateInstance(DateFormat.LONG)
    private val tf = DateFormat.getTimeInstance(DateFormat.DEFAULT)

    private val homeFragment = HomeFragment()
    private val timetableFragment = TimetableFragment()
    private val calendarFragment = CalendarFragment()
    private val batteryFragment = BatteryFragment()
    private val settingsFragment = SettingsFragment()
    private var active: Fragment = homeFragment
    private val fm = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar as Toolbar?)
        bottomNavigation.setOnNavigationItemSelectedListener(navListener)
        fm.beginTransaction().add(R.id.fragment_container, settingsFragment, "5").hide(settingsFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, batteryFragment, "4").hide(batteryFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, calendarFragment, "3").hide(calendarFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, timetableFragment, "2").hide(timetableFragment).commit()
        fm.beginTransaction().add(R.id.fragment_container, homeFragment, "1").commit()

        startService(intentFor<IQService>(IQService.ACTION to IQService.NEW_PENDING_INTENT, IQService.PENDING_INTENT to createPendingResult(1, intent, 0)))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_about -> startActivity(intentFor<AboutActivity>())
        }
        return super.onOptionsItemSelected(item)
    }

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.action_home -> {
                fm.beginTransaction().hide(active).show(homeFragment).commit()
                active = homeFragment
            }
            R.id.action_settings -> {
                fm.beginTransaction().hide(active).show(settingsFragment).commit()
                active = settingsFragment
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
        return@OnNavigationItemSelectedListener true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Yeah! request: $requestCode result: $resultCode")
        when(resultCode){
            IQService.GET_INFO_RESULT -> {
                //getDeviceInfo(data!!.getByteArrayExtra(IQService.DATA)!!)

            }
            IQService.RECONNECTING, IQService.BT_OFF -> {
                goingBack = true
                startActivity(intentFor<LoginActivity>(LoginActivity.KEY to resultCode).newTask().clearTask().clearTop())
            }
        }
    }/*



    fun getInfo(view: View){
        startService(intentFor<IQService>(IQService.ACTION to IQService.DATA_TRANSFER, IQService.TASK to 0, IQService.DATA to ByteArray(1)))
    }

    private fun getDeviceInfo(data: ByteArray){
        currentDateTime.timeInMillis = (getLongFromByteArray(data) - 10_800)*1_000 //-3 h (Arduino stores MSC time, Android - UTC)
        current_time.text = tf.format(currentDateTime.time)
        current_date.text = df.format(currentDateTime.time)

    }

    fun timePicker(view: View){
        val dialog = TimePickerDialog(this, android.R.style.ThemeOverlay_Material_Dialog,
            TimePickerDialog.OnTimeSetListener{_, mHour, mMinute -> run{
                newDateTime.set(Calendar.HOUR_OF_DAY, mHour)
                newDateTime.set(Calendar.MINUTE, mMinute)
                newDateTime.set(Calendar.SECOND, 0)
                new_time.text = tf.format(newDateTime.time)
            }}, currentDateTime.get(Calendar.HOUR_OF_DAY), currentDateTime.get(Calendar.MINUTE), true)
        dialog.show()
    }

    fun datePicker(view: View){
        val dialog = DatePickerDialog(this, android.R.style.ThemeOverlay_Material_Dialog,
            DatePickerDialog.OnDateSetListener{_, mYear, mMonth, mDay -> run{
                newDateTime.set(Calendar.YEAR, mYear)
                newDateTime.set(Calendar.MONTH, mMonth)
                newDateTime.set(Calendar.DAY_OF_MONTH, mDay)
                new_date.text = df.format(newDateTime.time)
            }}, currentDateTime.get(Calendar.YEAR), currentDateTime.get(Calendar.MONTH),
            currentDateTime.get(Calendar.DAY_OF_MONTH))
        dialog.show()
    }

    fun sendTime(view: View){
        val timeForArduino = newDateTime.timeInMillis / 1_000 + 10_800
        val data = ByteArray(1){0x4} + makeByteArrayFromLong(timeForArduino)
        startService(intentFor<IQService>(IQService.ACTION to IQService.DATA_TRANSFER, IQService.TASK to 4, IQService.DATA to data))
    }

    fun setSysTime(view: View){
        newDateTime.timeInMillis = System.currentTimeMillis()
        new_date.text = df.format(newDateTime.time)
        new_time.text = tf.format(Date())
    }

    private fun getLongFromByteArray(data: ByteArray): Long{
        var result = 0u
        for(i in 3 downTo 0) result = (result shl 8) + data[i].toUByte()
        return result.toLong()
    }

    private fun makeByteArrayFromLong(sum: Long): ByteArray{
        val result = ByteArray(4)
        val tempSum = sum.toUInt()
        for(i in 0..3) result[i] = (tempSum shr 8*i).toUByte().toByte()
        return result
    }*/

    override fun onDestroy() {
        super.onDestroy()
        if(!goingBack) startService(intentFor<IQService>(IQService.ACTION to IQService.STOP_SERVICE))
    }
}
