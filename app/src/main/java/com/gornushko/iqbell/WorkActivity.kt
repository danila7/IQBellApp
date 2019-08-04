package com.gornushko.iqbell

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

class WorkActivity : AppCompatActivity() {

    companion object Const{
        private const val TAG = "Bluetooth Action"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work)
        val pIntent = createPendingResult(1, intent, 0)
        val launchServiceIntent = Intent(this, IQService::class.java)
            .putExtra(IQService.ACTION, IQService.NEW_PERNDING_INTENT)
            .putExtra(IQService.PENDING_INTENT, pIntent)
        startService(launchServiceIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "Yeah! request: $requestCode result: $resultCode")
        when(resultCode){

            IQService.BT_OFF -> Toast.makeText(this, "BT IS OFF!", Toast.LENGTH_LONG).show()

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startService(Intent(this, IQService::class.java).putExtra(IQService.ACTION, IQService.STOP_SERVICE))
    }
}
