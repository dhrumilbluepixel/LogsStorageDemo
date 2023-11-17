package com.example.logsstoragedemo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.logsstoragedemo.background.AlarmReceiver
import com.example.logsstoragedemo.background.WorkManagerScheduler
import com.example.logsstoragedemo.utils.Constants
import com.example.logsstoragedemo.utils.Constants.IS_SCHEDULED
import com.example.logsstoragedemo.utils.FileLogger
import com.example.logsstoragedemo.utils.SharedPrefsManager
import com.example.logsstoragedemo.utils.SharedPrefsManager.get
import com.example.logsstoragedemo.utils.SharedPrefsManager.set
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    private val TAG: String? = javaClass.simpleName
    var permissionsList: ArrayList<String>? = null
    var permissionsStr = arrayOf<String>(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
    var permissionsCount = 0

    private var permissionsLauncher = registerForActivityResult<Array<String>, Map<String, Boolean>>(
            ActivityResultContracts.RequestMultiplePermissions(),
            ActivityResultCallback<Map<String, Boolean>> { result ->
                val list = ArrayList(result.values)
                permissionsList = ArrayList()
                permissionsCount = 0
                for (i in list.indices) {
                    if (shouldShowRequestPermissionRationale(permissionsStr[i])) {
                        permissionsList!!.add(permissionsStr[i])
                    } else if (!hasPermission(this@MainActivity, permissionsStr[i])) {
                        permissionsCount++
                    }
                }
                if (permissionsList!!.size > 0) {
                    //Some permissions are denied and can be asked again.
                    askForPermissions(permissionsList!!)
                } else if (permissionsCount > 0) {
                    //Show alert dialog
                    showPermissionDialog()
                } else {
                    //All permissions granted. Do your stuff
                    Log.e(TAG, "All required permissions are granted")

                    val isScheduled: Boolean? = SharedPrefsManager.customPrefs?.get(IS_SCHEDULED, false)
                    isScheduled?.let { scheduled ->
                        if (!scheduled) {
                            Log.e(TAG, "Scheduled at 12 AM")
                            SharedPrefsManager.customPrefs?.set(Constants.IS_SCHEDULED, true) //setter
                            triggerAlarmOnceInADay()
                        } else{
                            Log.e(TAG, "Already scheduled")
                        }
                    }
                }
            })

    private fun hasPermission(context: Context, permissionStr: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permissionStr
        ) == PackageManager.PERMISSION_GRANTED
    }

    var alertDialog: AlertDialog? = null

    private fun showPermissionDialog() {
        Log.e(TAG, "Display permission dialog")

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Permission required")
            .setMessage("Some permissions are needed to be allowed to use this app without any problems.")
            .setPositiveButton("Ok") { dialog, which -> dialog.dismiss() }
        if (alertDialog == null) {
            alertDialog = builder.create()
            if (!alertDialog!!.isShowing) {
                alertDialog!!.show()
            }
        }
    }

    private fun askForPermissions(permissionsList: ArrayList<String>) {
        val newPermissionStr: Array<String> = permissionsList.toTypedArray()
        for (i in newPermissionStr.indices) {
            newPermissionStr[i] = permissionsList[i]
        }
        if (newPermissionStr.isNotEmpty()) {
            permissionsLauncher.launch(newPermissionStr)
        } else {
            /* User has pressed 'Deny & Don't ask again' so we have to show the enable permissions dialog
            which will lead them to app details page to enable permissions from there. */
            showPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initialize shared preferences
        SharedPrefsManager.init(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissionsList = ArrayList()
            permissionsList!!.addAll(permissionsStr.asList())

            Log.e(TAG, "Check required permissions")
            askForPermissions(permissionsList!!)
        } else{
            // check manage external storage permission for android 13
//            if (!Environment.isExternalStorageManager()) {
//                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                val uri = Uri.fromParts("package", packageName, null)
//                intent.data = uri
//                startActivity(intent)
//            }

            val isScheduled: Boolean? = SharedPrefsManager.customPrefs?.get(IS_SCHEDULED, false)
            isScheduled?.let { scheduled ->
                if (!scheduled) {
                    Log.e(TAG, "Scheduled at 12 AM")
                    SharedPrefsManager.customPrefs?.set(IS_SCHEDULED, true) //setter
                    triggerAlarmOnceInADay()
                } else{
                    Log.e(TAG, "Already scheduled")
                }
            }
        }

    }

    private fun triggerAlarmOnceInADay(){
        FileLogger.init(this)

        val cal: Calendar = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 19) // set hour
        cal.set(Calendar.MINUTE, 14) // set minute

        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val time: Long = cal.timeInMillis - (cal.timeInMillis % 60000)

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
    }

}