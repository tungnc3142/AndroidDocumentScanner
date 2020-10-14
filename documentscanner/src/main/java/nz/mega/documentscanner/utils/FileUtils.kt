package nz.mega.documentscanner.utils

import android.content.Context
import java.io.File

object FileUtils {

    const val FILE_NAME_FORMAT = "Scanned_%1tY%<tm%<td%<tH%<tM"

    private const val ROOT_FILE_DIR = "scans"
    private const val PAGE_FILE_DIR = "$ROOT_FILE_DIR/pages/"
    private const val DOCUMENT_FILE_DIR = "$ROOT_FILE_DIR/document/"

    private fun getParentFile(context: Context, parentDir: String): File =
        File(context.filesDir, parentDir).apply {
            if (!exists()) {
                mkdirs()
            }
        }

    fun createDocumentFile(context: Context, title: String): File =
        File(getParentFile(context, DOCUMENT_FILE_DIR), title).apply {
            if (exists()) {
                delete()
                createNewFile()
            }
        }

    fun createPageFile(context: Context): File =
        File(getParentFile(context, PAGE_FILE_DIR), System.currentTimeMillis().toString())

    fun createPhotoFile(context: Context): File =
        File(context.cacheDir, System.currentTimeMillis().toString())

    fun clearExistingFiles(context: Context): Boolean =
        getParentFile(context, ROOT_FILE_DIR).deleteRecursively()

    fun getProviderAuthority(context: Context): String =
        "${context.packageName}.scans.provider"
}
