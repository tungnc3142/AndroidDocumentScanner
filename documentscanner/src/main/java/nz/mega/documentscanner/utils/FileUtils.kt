package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.BitmapFactory
import nz.mega.documentscanner.utils.ImageUtils.rotate
import nz.mega.documentscanner.utils.ImageUtils.toFile
import java.io.File

object FileUtils {

    const val FILE_NAME_FORMAT = "Scanned_%1tY%<tm%<td%<tH%<tM.pdf"
    private const val FILE_ROOT_DIR = "scans"

    fun getParentFile(context: Context): File =
        File(context.filesDir, FILE_ROOT_DIR).apply {
            if (!exists()) {
                mkdir()
            }
        }

    fun createNewFile(
        context: Context,
        title: String = System.currentTimeMillis().toString()
    ): File =
        File(getParentFile(context), title)

    fun clearExistingFiles(context: Context): Boolean =
        getParentFile(context).deleteRecursively()

    fun File.rotate(degrees: Float): File {
        val bitmap = BitmapFactory.decodeFile(path)
        val rotatedBitmap = bitmap.rotate(degrees)
        return rotatedBitmap.toFile(this)
    }
}
