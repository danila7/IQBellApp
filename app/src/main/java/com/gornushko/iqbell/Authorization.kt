package com.gornushko.iqbell

import android.content.Intent
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference

class Authorization internal constructor(context: MainActivity) : AsyncTask<ByteArray, Void, Boolean>() {

    private val activityReference: WeakReference<MainActivity> = WeakReference(context)

    override fun doInBackground(vararg p0: ByteArray?): Boolean {
        val oStream = BluetoothService.socket!!.outputStream
        val iStream = BluetoothService.socket!!.inputStream
        try{
            oStream.write(p0[0]!!)
        }catch (e: IOException){
            Log.e("Sending Password", "Connection is lost")
            return false
        }
        Thread.sleep(3_000)
        var code = 0
        if(iStream.available() > 0){
            val available = iStream.available()
             code = iStream.read()
            while (iStream.available() > 0) iStream.read()
        }
        return (code == 82)
    }

    override fun onPreExecute() {
        super.onPreExecute()
        val activity = activityReference.get()
        activity?.login_button?.visibility = View.GONE
        activity?.progress?.visibility = View.VISIBLE
    }

    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        val activity = activityReference.get()
        activity?.progress?.visibility = View.INVISIBLE
        if(result!!){
            val intent = Intent(activity, WorkActivity::class.java)
            activity?.startActivity(intent)
        }
        else Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show()
        activity?.login_button?.visibility = View.VISIBLE
    }
}