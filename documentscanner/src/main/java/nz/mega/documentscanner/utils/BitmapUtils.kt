package nz.mega.documentscanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.IntRange
import androidx.camera.core.ImageProxy
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSources
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.facebook.imageutils.BitmapUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

object BitmapUtils {

    /**
     * Get Bitmap image from provided uri in a sync way.
     *
     * @param imageUri Uri to get the image from
     * @param degreesToRotate Degrees to clockwise rotate the image
     * @param quality  Image quality (0-100) to render the image. 0 meaning compress for
     *                 small size, 100 meaning compress for max quality
     * @return Bitmap image
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun getBitmapFromUri(
        imageUri: Uri,
        degreesToRotate: Int = 0,
        @IntRange(from = 0, to = 100) quality: Int = 100
    ): Bitmap = withContext(Dispatchers.Default) {
        val dimensions = BitmapUtil.decodeDimensions(imageUri)!!
        val imageWidth = dimensions.first
        val imageHeight = dimensions.second
        val maxSize = max(imageWidth, imageHeight)

        val imageRequest = ImageRequestBuilder.newBuilderWithSource(imageUri)
            .setResizeOptions(ResizeOptions(imageWidth, imageHeight, maxSize.toFloat()))

        if (degreesToRotate != 0) {
            imageRequest.rotationOptions = RotationOptions.forceRotation(degreesToRotate)
        }

        val dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest.build(), this)
        val result = DataSources.waitForFinalResult(dataSource) as CloseableReference<CloseableBitmap>
        val resultBitmap = result.get().underlyingBitmap.compress(quality)

        CloseableReference.closeSafely(result)
        dataSource.close()

        return@withContext resultBitmap
    }

    /**
     * Compress Bitmap image with the provided quality in a sync way.
     *
     * @param quality  Hint to the compressor, 0-100. 0 meaning compress for
     *                 small size, 100 meaning compress for max quality
     */
    suspend fun Bitmap.compress(quality: Int) =
        withContext(Dispatchers.Default) {
            val outputStream = ByteArrayOutputStream()
            compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            BitmapFactory.decodeStream(ByteArrayInputStream(outputStream.toByteArray()))
        }

    /**
     * Rotate Bitmap image with the provided degrees in a sync way.
     *
     * @param degrees to be rotated clockwise.
     * @return Rotated Bitmap image
     */
    suspend fun Bitmap.rotate(degrees: Int): Bitmap =
        withContext(Dispatchers.Default) {
            if (degrees == 0) return@withContext this@rotate

            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(this@rotate, 0, 0, width, height, matrix, true).also {
                this@rotate.recycle()
            }
        }

    /**
     * Convert provided ImageProxy to Bitmap in a sync way.
     *
     * @return Bitmap image
     */
    suspend fun ImageProxy.toBitmap(): Bitmap =
        withContext(Dispatchers.Default) {
            require(format == ImageFormat.JPEG)

            val buffer = planes[0].buffer
            buffer.rewind()
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bitmap.rotate(imageInfo.rotationDegrees)
        }

    /**
     * Convert provided Bitmap Image to OpenCV Mat
     *
     * @return OpenCV Mat
     */
    fun Bitmap.toMat(): Mat {
        val mat = Mat()
        Utils.bitmapToMat(this, mat)
        return mat
    }

    /**
     * Convert provided OpenCV Mat to Bitmap image.
     *
     * @return Bitmap image
     */
    fun Mat.toBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(this, bitmap)
        return bitmap
    }
}
