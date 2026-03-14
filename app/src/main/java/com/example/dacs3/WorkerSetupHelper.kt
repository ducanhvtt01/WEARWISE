//Chuyên lo việc lập lịch chạy ngầm khi rời ứng dụng
package com.example.dacs3

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dacs3.dashboard.DailyOutfitWorker
import com.example.dacs3.dashboard.WeatherMonitorWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

object WorkerSetupHelper {

    fun setupWeatherMonitor(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val weatherWorkRequest = PeriodicWorkRequestBuilder<WeatherMonitorWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "WeatherMonitorTask",
            ExistingPeriodicWorkPolicy.KEEP,
            weatherWorkRequest
        )
    }

    fun setupDailyOutfitNotification(context: Context) {
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()

        dueDate.set(Calendar.HOUR_OF_DAY, 7)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)

        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.HOUR_OF_DAY, 24)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyOutfitWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "DailyOutfitTask",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )
    }
}