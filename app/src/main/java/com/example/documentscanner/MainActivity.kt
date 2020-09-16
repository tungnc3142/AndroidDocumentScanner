package com.example.documentscanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import nz.mega.documentscanner.DocumentScannerActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_main)

        Handler().postDelayed({
            startActivity(Intent(this, DocumentScannerActivity::class.java))
        }, 2000L)
    }
}
