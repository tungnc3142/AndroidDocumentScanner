package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.BitmapFactory
import nz.mega.documentscanner.BuildConfig
import nz.mega.documentscanner.utils.ImageUtils.rotate
import nz.mega.documentscanner.utils.ImageUtils.toFile
import java.io.File

object FileUtils {

    const val FILE_NAME_FORMAT = "Scanned_%1tY%<tm%<td%<tH%<tM"
    const val JPG_SUFFIX = ".jpg"
    const val PDF_SUFFIX = ".pdf"
    const val PROVIDER_AUTHORITY = BuildConfig.LIBRARY_PACKAGE_NAME + ".fileprovider"

    private const val ROOT_FILE_DIR = "scans"
    private const val PAGE_FILE_DIR = "$ROOT_FILE_DIR/pages/"
    private const val DOCUMENT_FILE_DIR = "$ROOT_FILE_DIR/document/"

    private fun getParentFile(context: Context, parentDir: String): File =
        File(context.filesDir, parentDir).apply {
            if (!exists()) {
                mkdirs()
            }
        }

    fun createPageFile(context: Context): File =
        File(getParentFile(context, PAGE_FILE_DIR), System.currentTimeMillis().toString()).apply {
            if (exists()) {
                delete()
                createNewFile()
            }
        }

    fun createDocumentFile(context: Context, title: String): File =
        File(getParentFile(context, DOCUMENT_FILE_DIR), title).apply {
            if (exists()) {
                delete()
                createNewFile()
            }
        }

    fun clearExistingFiles(context: Context): Boolean =
        getParentFile(context, ROOT_FILE_DIR).deleteRecursively()

    fun File.rotate(degrees: Float): File {
        val bitmap = BitmapFactory.decodeFile(path)
        val rotatedBitmap = bitmap.rotate(degrees)
        return rotatedBitmap.toFile(this)
    }
}
