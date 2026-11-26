package com.example

import android.app.Application
import com.example.core.di.AppContainer
import com.example.core.reminders.ReminderChannels

class MyCareApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderChannels.ensureChannels(this)
    }
}
