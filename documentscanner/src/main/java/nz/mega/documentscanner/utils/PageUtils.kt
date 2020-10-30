package nz.mega.documentscanner.utils

import android.content.Context
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.openCV.OpenCvUtils.rotate
import nz.mega.documentscanner.utils.ImageUtils.deleteFile
import nz.mega.documentscanner.utils.ImageUtils.rotate

object PageUtils {

    suspend fun Page.rotate(context: Context): Page {
        val originalImageRotated = originalImage.rotate(context)
        val croppedImageRotated = croppedImage?.rotate(context)
        val rotatedCropPoints = cropPoints?.rotate()

        deleteFiles()

        return copy(
            originalImage = originalImageRotated,
            croppedImage = croppedImageRotated,
            cropPoints = rotatedCropPoints
        )
    }

    suspend fun Page.deleteFiles() {
        originalImage.deleteFile()
        croppedImage?.deleteFile()
    }
}
