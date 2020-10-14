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
import nz.mega.documentscanner.utils.BitmapUtils.toFile
import java.io.FileOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
object DocumentGenerator {

    suspend fun Document.generatePdf(context: Context): Uri =
        withContext(Dispatchers.Default) {
            require(pages.isNotEmpty()) { "Empty pages" }

            val pdfDocument = PdfDocument()
            val backgroundPaint = Paint().apply { color = Color.WHITE }

            pages.forEachIndexed { index, page ->
                val bitmap = BitmapUtils.getBitmapFromUri(
                    context = context,
                    uri = page.getImageToPrint().imageUri,
                    quality = quality.value,
                )

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

            val documentFile = FileUtils.createDocumentFile(context, title + PDF_SUFFIX)

            FileOutputStream(documentFile).apply {
                pdfDocument.writeTo(this)
                flush()
                close()
            }

            pdfDocument.close()

            documentFile.toUri()
        }

    suspend fun Document.generateJpg(context: Context): Uri =
        withContext(Dispatchers.IO) {
            require(pages.isNotEmpty())

            val pageUri = pages.first().getImageToPrint().imageUri

            val bitmap = Glide.with(context)
                .asBitmap()
                .load(pageUri)
                .encodeQuality(quality.value)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .submit()
                .get()

            val documentFile = FileUtils.createDocumentFile(context, title + FileUtils.JPG_SUFFIX)

            bitmap.toFile(documentFile)
            bitmap.recycle()

            documentFile.toUri()
        }
}
