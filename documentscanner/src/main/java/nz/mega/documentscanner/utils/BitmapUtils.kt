package nz.mega.documentscanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.IntRange
import com.facebook.datasource.DataSources
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.request.ImageRequestBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object BitmapUtils {

    suspend fun getBitmapFromUri(
        imageUri: Uri,
        degreesToRotate: Int = 0,
        @IntRange(from = 0, to = 100) quality: Int = 100
    ): Bitmap =
        withContext(Dispatchers.IO) {
            val imageRequest = ImageRequestBuilder.newBuilderWithSource(imageUri)
            if (degreesToRotate != 0) {
                imageRequest.rotationOptions = RotationOptions.forceRotation(degreesToRotate)
            }

            val dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest.build(), this)

            val result = DataSources.waitForFinalResult(dataSource)
            val resultBitmap = (result?.get() as? CloseableBitmap?)?.underlyingBitmap!!
            dataSource.close()

            val out = ByteArrayOutputStream()
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            BitmapFactory.decodeStream(ByteArrayInputStream(out.toByteArray()))
        }

    fun Bitmap.toMat(): Mat {
        val mat = Mat(height, width, CvType.CV_8U, Scalar(4.0))
        val bitmap32 = copy(Bitmap.Config.ARGB_8888, true)
        Utils.bitmapToMat(bitmap32, mat)
        return mat
    }

    fun Mat.toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(this, bitmap)
        return bitmap
    }

    suspend fun Bitmap.rotate(degrees: Int): Bitmap = withContext(Dispatchers.Default) {
        if (degrees == 0) return@withContext this@rotate

        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(this@rotate, 0, 0, width, height, matrix, true)
        recycle()
        return@withContext rotated
    }
}
