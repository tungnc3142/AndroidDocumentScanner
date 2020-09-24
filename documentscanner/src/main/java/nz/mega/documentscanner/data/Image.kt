package nz.mega.documentscanner.data

import android.net.Uri

data class Image constructor(
    var imageUri: Uri,
    var width: Float,
    var height: Float
)
