package nz.mega.documentscanner.camera

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nz.mega.documentscanner.utils.ImageScanner
import nz.mega.documentscanner.utils.ImageUtils.rotate
import nz.mega.documentscanner.utils.ImageUtils.toYuvBitmap

class DocumentAnalyzer(
    private val coroutineScope: CoroutineScope,
    private val listener: (List<PointF>, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "DocumentAnalyzer"
    }

    private val imageScanner: ImageScanner by lazy { ImageScanner() }

    override fun analyze(imageProxy: ImageProxy) {
        coroutineScope.launch {
            try {
                val sourceBitmap = imageProxy.image!!.toYuvBitmap().rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                val points = imageScanner.getCropPoints(sourceBitmap)

                listener.invoke(points, sourceBitmap.width, sourceBitmap.height)

                sourceBitmap.recycle()
            } catch (error: Exception) {
                Log.w(TAG, error.stackTraceToString())
            } finally {
                imageProxy.close()
            }
        }
    }
}
