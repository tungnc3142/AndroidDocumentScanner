package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.data.Document
import java.io.FileOutputStream

object PdfUtils {

    suspend fun Document.generatePdf(context: Context): Uri =
        withContext(Dispatchers.IO) {
            require(pages.isNotEmpty()) { "Empty pages" }

            val pdfDocument = PdfDocument()
            val paint = Paint().apply { color = Color.WHITE }

            pages.forEachIndexed { index, page ->
                val pageFile = (page.croppedImageUri ?: page.originalImageUri).toFile()

                val bitmap = BitmapFactory.decodeFile(
                    pageFile.absolutePath,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                )

                val pdfPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index).create()
                )

                pdfPage.canvas.apply {
                    drawPaint(paint)
                    drawBitmap(bitmap, 0f, 0f, null)
                }

                pdfDocument.finishPage(pdfPage)
            }

            val documentFile = FileUtils.createNewFile(context, title)

            FileOutputStream(documentFile).apply {
                pdfDocument.writeTo(this)
                flush()
                close()
            }

            pdfDocument.close()

            documentFile.toUri()
        }
}
