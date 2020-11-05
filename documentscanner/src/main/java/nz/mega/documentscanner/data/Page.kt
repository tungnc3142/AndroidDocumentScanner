package nz.mega.documentscanner.data

import android.net.Uri
import org.opencv.core.MatOfPoint2f

data class Page constructor(
    val id: Long = System.currentTimeMillis(),
    val imageUri: Uri,
    var cropMat: MatOfPoint2f?
)
