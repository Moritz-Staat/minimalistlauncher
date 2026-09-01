package de.moritzstaat.launcher.data.weather

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.moritzstaat.launcher.LauncherApplication
import java.util.concurrent.TimeUnit

/**
 * Keeps the cached reading warm while the launcher is not running.
 *
 * Hourly is as often as Open-Meteo has anything new to say, and WorkManager is what survives
 * the process being killed. The home screen refreshes on its own when it comes to the front.
 */
class WeatherRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val services = (applicationContext as? LauncherApplication)?.services
            ?: return Result.success()
        // A failed request is not an error worth retrying immediately: the next run is due in
        // an hour anyway, and a retry storm without network costs battery for nothing.
        services.weatherRepository.refresh()
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "weather-refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(
                INTERVAL_MINUTES,
                TimeUnit.MINUTES,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        private const val INTERVAL_MINUTES = 60L
    }
}
