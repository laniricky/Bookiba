package co.booknook.app

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BookibaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager manually to handle any pending legacy jobs from OS
        // and cancel them. This prevents IllegalStateException when SystemJobService starts.
        try {
            val config = Configuration.Builder().build()
            WorkManager.initialize(this, config)
            WorkManager.getInstance(this).cancelAllWork()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
