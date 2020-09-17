package nz.mega.documentscanner

import android.content.Context
import android.util.Log
import nz.mega.documentscanner.utils.FileUtils
import org.opencv.android.OpenCVLoader

object DocumentScanner {

    private const val TAG = "DocumentScanner"
    private var isInitialized = false

    @JvmStatic
    fun initialize(context: Context) {
        initOpenCV()
        clearExistingFiles(context)
    }

    private fun initOpenCV() {
        isInitialized = OpenCVLoader.initDebug()
    }

    private fun clearExistingFiles(context: Context) {
        val result = FileUtils.clearExistingFiles(context)
        Log.d(TAG, "Cleared existing files: $result")
    }

    @JvmStatic
    fun isInitialized(): Boolean = isInitialized
}
