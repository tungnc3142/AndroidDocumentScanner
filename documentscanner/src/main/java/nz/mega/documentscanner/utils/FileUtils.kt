package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object FileUtils {

    const val FILE_NAME_PATTERN = "[\"*/:<>?|]"
    const val FILE_NAME_FORMAT = "Scanned_%1tY%<tm%<td%<tH%<tM%<tS"

    private const val ROOT_FILE_DIR = "scans"
    private const val PAGE_FILE_DIR = "$ROOT_FILE_DIR/pages/"
    private const val DOCUMENT_FILE_DIR = "$ROOT_FILE_DIR/document/"

    private suspend fun getParentFile(context: Context, parentDir: String): File =
        withContext(Dispatchers.IO) {
            File(context.cacheDir, parentDir).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        }

    suspend fun createDocumentFile(context: Context, title: String): File =
        withContext(Dispatchers.IO) {
            File(getParentFile(context, DOCUMENT_FILE_DIR), title).apply {
                if (exists()) {
                    delete()
                    createNewFile()
                }
            }
        }

    suspend fun createImageFile(context: Context, bitmap: Bitmap): File =
        withContext(Dispatchers.IO) {
            File(getParentFile(context, PAGE_FILE_DIR), System.currentTimeMillis().toString())
                .apply { bitmap.toFile(this) }
        }

    suspend fun clearExistingFiles(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            getParentFile(context, ROOT_FILE_DIR).deleteRecursively()
        }

    suspend fun File.deleteSafely(): Boolean =
        withContext(Dispatchers.IO) {
            delete()
        }

    suspend fun Bitmap.toFile(file: File) {
        withContext(Dispatchers.IO) {
            val outputStream = FileOutputStream(file)
            compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        }
    }

    suspend fun PdfDocument.toFile(file: File) {
        withContext(Dispatchers.IO) {
            val outputStream = FileOutputStream(file)
            writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
        }
    }

    fun getProviderAuthority(context: Context): String =
        "${context.packageName}.scans.provider"
}
