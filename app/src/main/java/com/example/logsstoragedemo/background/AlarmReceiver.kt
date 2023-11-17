package com.example.logsstoragedemo.background

import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.logsstoragedemo.utils.Constants.APP_NAME
import com.example.logsstoragedemo.utils.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar
import java.util.zip.GZIPOutputStream


class AlarmReceiver : BroadcastReceiver() {
    private val TAG: String? = javaClass.simpleName
    private var stringBuilderLog: StringBuilder = FileLogger.saveLog()
    private var stringBuilderDefault: StringBuilder = StringBuilder()

    override fun onReceive(ctx: Context?, alarmIntent: Intent?) {
        if ((Intent.ACTION_BOOT_COMPLETED) == alarmIntent?.action) {
            // reset all alarms
            scheduleAlarmAt12AM(ctx, alarmIntent)
        } else {
            // perform your scheduled task here
            val coroutineCallLogger = CoroutineScope(Dispatchers.IO)
            coroutineCallLogger.launch {
                async {
                    val file = File(ctx?.filesDir, "${APP_NAME}_USERID_${System.currentTimeMillis()}.zip")
                    if (file.exists()) {
                        file.delete()
                    }
                    saveDefaultFileDetails(file, ctx!!)
                }
            }
        }
    }

    // trigger alarm at 12 AM
    private fun scheduleAlarmAt12AM(ctx: Context?, alarmIntent: Intent?) {
        val cal: Calendar = Calendar.getInstance()

        // calendar is called to get current time in hour and minute
        cal.set(Calendar.HOUR_OF_DAY, 0) // set hour
        cal.set(Calendar.MINUTE, 0) // set minute

        val pendingIntent = alarmIntent?.let {
            PendingIntent.getBroadcast(ctx, 0,
                it, PendingIntent.FLAG_MUTABLE)
        }
        val time: Long = cal.timeInMillis - (cal.timeInMillis % 60000)

        val alarmManager = ctx?.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            time,
            pendingIntent
        )
        Log.e(TAG, "Reset alarm after device restart")
    }

    // store local files in local storage
    private suspend fun saveDefaultFileDetails(file: File, context: Context) {
        if (!file.exists()) {
            Log.e(TAG, "Create log file")
            withContext(Dispatchers.IO) {
                file.createNewFile()
            }

            Log.e(TAG, "Add device information in log file")
            takeUserInformationDetails(context)

            Log.e(TAG, "Add logs information in log file")
            stringBuilderDefault.append(stringBuilderLog)
            file.appendText(stringBuilderDefault.toString())

            // compress file
            compressFile(file, context)

            // clear logs from logcat
            FileLogger.clearLogs()
            Log.e(TAG, "Clear logs from logcat")

            // check internet connection
            Log.e(TAG, "Check internet connection")

            val connectionManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectionManager.activeNetworkInfo
            if(networkInfo != null && networkInfo.isConnectedOrConnecting){
                // Internet connection is available
                Log.e(TAG, "Internet connection is available")

                // upload logs files
                FileLogger.getAllFilesFromLocal(context)

            } else{
                // No internet connection available
                Log.e(TAG, "No internet connection available")

                WorkManagerScheduler.triggerScheduledWork(context)
            }
        }
    }

    private fun compressFile(file: File, ctx: Context){
        val fIn: FileInputStream = FileInputStream(file.absolutePath)
        val compressFile = File(ctx.filesDir, "${APP_NAME}_USERID_${System.currentTimeMillis()}.zip")
        val fOut: FileOutputStream = FileOutputStream(compressFile.absolutePath)
        val gZip = GZIPOutputStream(fOut)
        val buf = ByteArray(1024)
        var readCount = fIn.read(buf)
        while (readCount > 0) {
            gZip.write(buf, 0, readCount)
            readCount = fIn.read(buf)
        }
        gZip.finish()
        gZip.close()
        fIn.close()

        // delete text logs files in local storage
        if (file.exists()) {
            file.delete()
            Log.e(TAG, "Delete text file from Local Storage")
        }
    }

    private fun takeUserInformationDetails(context: Context) {
        stringBuilderDefault = StringBuilder()
        stringBuilderDefault.append(
            "User Information:" + "\n"
                    + "USER ID:" + 1 + "\n"
                    + "EMAIL: " + "johndoe@gmail.com" + "\n"
                    + "NAME:" + "John Doe" + "\n"
        )
        takeDeviceInformationDetails(context)
    }

    private fun takeDeviceInformationDetails(context: Context) {
        val deviceId = Build.ID
        val deviceSerial = Build.FINGERPRINT
        val device = Build.DEVICE
        val deviceModel = Build.MODEL
        val deviceType = Build.TYPE
        val deviceUser = Build.USER
        val sdkVersion = Build.VERSION.SDK_INT
        val manufacturer = Build.MANUFACTURER
        val host = Build.HOST
        val hardware = Build.HARDWARE
        val deviceBrand = Build.BRAND
        val product = Build.PRODUCT

        stringBuilderDefault.append(
            "Device Information:" + "\n"
                    + "ID:" + deviceId + "\n"
                    + "SERIAL: " + deviceSerial + "\n"
                    + "DEVICE:" + device + "\n"
                    + "DEVICE MODEL:" + deviceModel + "\n"
                    + "DEVICE TYPE:" + deviceType + "\n"
                    + "USER:" + deviceUser + "\n"
                    + "SDK VERSION:" + sdkVersion + "\n"
                    + "MANUFACTURER:" + manufacturer + "\n"
                    + "HOST:" + host + "\n"
                    + "HARDWARE:" + hardware + "\n"
                    + "BRAND:" + deviceBrand + "\n"
                    + "PRODUCT:" + product + "\n"
        )
        takeDevicePerformanceDetails(context)
    }

    private fun takeDevicePerformanceDetails(context: Context) {
        try {
            val stringBuilderCpuAbi: StringBuilder = StringBuilder()
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager: ActivityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            val runtime: Runtime = Runtime.getRuntime()
            val availableMemory = memoryInfo.availMem / 1048576L
            val totalMemory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                memoryInfo.totalMem / 1048576L
            } else {
                null
            }
            val lowMemory = memoryInfo.lowMemory
            val runtimeMaxMemory = runtime.maxMemory() / 1048576L
            val runtimeTotalMemory = runtime.totalMemory() / 1048576L
            val runtimeFreeMemory = runtime.freeMemory() / 1048576L
            val availableProcessors = runtime.availableProcessors()
            val usedMemorySize = (runtimeTotalMemory - runtimeFreeMemory)
            val cpuAbi: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                for (cpuAbiItem in Build.SUPPORTED_ABIS.iterator()) {
                    stringBuilderCpuAbi.append(cpuAbiItem + "\n")
                }
                stringBuilderCpuAbi.toString()
            } else {
                Build.CPU_ABI.toString()
            }
            val sendNetworkUsage = android.net.TrafficStats.getMobileTxBytes()
            val receivedNetworkUsage = android.net.TrafficStats.getMobileRxBytes()
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            var batteryLevel = -1
            var batteryScale = 1
            if (batteryStatus != null) {
                batteryLevel =
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, batteryLevel)
                batteryScale =
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, batteryScale)
            }
            val battery = batteryLevel / batteryScale.toFloat() * 100
            stringBuilderDefault.append(
                "Available Memory:$availableMemory MB\nTotal Memory:$totalMemory MB\nRuntime Max Memory: $runtimeMaxMemory MB \n" +
                        "Runtime Total Memory:$runtimeTotalMemory MB\nRuntime Free Memory:$runtimeFreeMemory MB\nLow Memory: ${
                            lowMemory.toString().trim()
                        }\nAvailable Processors:$availableProcessors\n"
                        + "Used Memory Size:$usedMemorySize MB\nCPU ABI:${cpuAbi.trim()}\nNetwork Usage(Send):$sendNetworkUsage Bytes\nNetwork Usage(Received):$receivedNetworkUsage Bytes\n"
                        + "Battery:${battery.toString().trim()}\n "
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}