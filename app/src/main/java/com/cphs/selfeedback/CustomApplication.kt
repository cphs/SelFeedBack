package com.cphs.selfeedback

import android.app.Application
import com.google.firebase.FirebaseApp

class CustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(applicationContext)
    }
}