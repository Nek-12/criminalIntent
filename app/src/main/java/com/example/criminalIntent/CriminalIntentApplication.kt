package com.example.criminalIntent

import android.app.Application

class CriminalIntentApplication: Application() {
        override fun onCreate() {
            super.onCreate()
            CrimeRepository.init(this)
        }
    }
