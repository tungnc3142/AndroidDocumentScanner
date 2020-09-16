package nz.mega.documentscanner

import android.content.Context
import android.util.Log
import nz.mega.documentscanner.utils.FileUtils
import org.opencv.android.InstallCallbackInterface
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.LoaderCallbackInterface.SUCCESS
import org.opencv.android.OpenCVLoader
import org.opencv.android.OpenCVLoader.OPENCV_VERSION

object DocumentScanner {

    private const val TAG = "DocumentScanner"
    private var isInitialized = false

    fun initialize(context: Context) {
        initOpenCV(context)
        clearExistingFiles(context)
    }

    private fun initOpenCV(context: Context) {
        if (BuildConfig.DEBUG) {
            isInitialized = OpenCVLoader.initDebug()
        } else {
            OpenCVLoader.initAsync(OPENCV_VERSION, context, object : LoaderCallbackInterface {
                override fun onManagerConnected(status: Int) {
                    Log.d(TAG, "OpenCV onManagerConnected success: ${status == SUCCESS}")
                    isInitialized = status == SUCCESS
                }

                override fun onPackageInstall(operation: Int, callback: InstallCallbackInterface?) {
                    Log.d(TAG, "OpenCV onPackageInstall operation: ${operation == SUCCESS}")
                }
            })
        }
    }

    private fun clearExistingFiles(context: Context) {
        val result = FileUtils.clearExistingFiles(context)
        Log.d(TAG, "Cleared existing files: $result")
    }

    fun isInitialized(): Boolean = isInitialized
}
