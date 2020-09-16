package com.example.documentscanner

import android.app.Application
import nz.mega.documentscanner.DocumentScanner

class ExampleApp : Application() {

    companion object {
        private const val TAG = "ExampleApp"
    }

    override fun onCreate() {
        super.onCreate()
        DocumentScanner.initialize(this)
    }
}
