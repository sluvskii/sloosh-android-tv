package com.sloosh.tv

import android.app.Application
import com.sloosh.tv.data.db.AppDatabase

class SlooshApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getDatabase(this)
    }
}
