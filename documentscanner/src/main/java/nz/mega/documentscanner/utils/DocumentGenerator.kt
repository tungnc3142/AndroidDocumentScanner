package nz.mega.documentscanner.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.data.Document.FileType
import nz.mega.documentscanner.utils.FileUtils.toFile
import nz.mega.documentscanner.utils.PageUtils.getCroppedBitmap

@Suppress("BlockingMethodInNonBlockingContext")
object DocumentGenerator {

    suspend fun Document.generatePdf(context: Context): Uri =
        withContext(Dispatchers.Default) {
            require(pages.isNotEmpty()) { "Empty pages" }

            val pdfDocument = PdfDocument()
            val backgroundPaint = Paint().apply { color = Color.WHITE }

            pages.forEachIndexed { index, page ->
                val bitmap = page.getCroppedBitmap(context)

                val pdfPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index).create()
                )

                pdfPage.canvas.apply {
                    drawPaint(backgroundPaint)
                    drawBitmap(bitmap, 0f, 0f, null)
                }

                pdfDocument.finishPage(pdfPage)
                bitmap.recycle()
            }

            val documentFile = FileUtils.createDocumentFile(context, title + FileType.PDF.suffix)
            pdfDocument.toFile(documentFile)
            pdfDocument.close()

            documentFile.toUri()
        }

    suspend fun Document.generateJpg(context: Context): Uri =
        withContext(Dispatchers.Default) {
            require(pages.isNotEmpty()) { "Empty pages" }

            val bitmap = pages.first().getCroppedBitmap(context)
            val documentFile = FileUtils.createDocumentFile(context, title + FileType.JPG.suffix)
            bitmap.toFile(documentFile)
            bitmap.recycle()

            documentFile.toUri()
        }
}
