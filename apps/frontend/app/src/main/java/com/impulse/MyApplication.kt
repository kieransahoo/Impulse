package com.impulse

import android.app.Application

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // App-level initialization (DI, analytics, etc.) goes here
    }
}
