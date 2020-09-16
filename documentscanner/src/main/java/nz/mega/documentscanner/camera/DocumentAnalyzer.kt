package nz.mega.documentscanner.camera

import android.graphics.PointF
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import nz.mega.documentscanner.utils.ImageScanner
import nz.mega.documentscanner.utils.ImageUtils.rotate
import nz.mega.documentscanner.utils.ImageUtils.toYuvBitmap

class DocumentAnalyzer(
    private val listener: (List<PointF>, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val imageScanner: ImageScanner by lazy { ImageScanner() }

    override fun analyze(imageProxy: ImageProxy) {
        processOverlay(imageProxy)
        imageProxy.close()
    }

    private fun processOverlay(imageProxy: ImageProxy) {
        try {
            val sourceBitmap = imageProxy.image!!.toYuvBitmap().rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            val points = imageScanner.getCropPoints(sourceBitmap).blockingGet()

            listener.invoke(points, sourceBitmap.width, sourceBitmap.height)

            sourceBitmap.recycle()
        } catch (exception: Exception) {
            Log.e("DocumentAnalyzer", exception.message.toString())
        }
    }
}
