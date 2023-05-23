package com.example.documentscanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import nz.mega.documentscanner.DocumentScannerActivity

class MainActivity : AppCompatActivity() {

     private val  REQUEST_CODE_SCAN_DOCUMENT: Int = 10;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = DocumentScannerActivity.getIntent(this)


        Handler().postDelayed({
            startActivityForResult(intent, REQUEST_CODE_SCAN_DOCUMENT)
        }, 2000L)
    }
}
