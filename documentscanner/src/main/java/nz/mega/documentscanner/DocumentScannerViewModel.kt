package nz.mega.documentscanner

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.data.Document.FileType
import nz.mega.documentscanner.data.Document.Quality
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.openCV.ImageScanner
import nz.mega.documentscanner.utils.BitmapUtils
import nz.mega.documentscanner.utils.DocumentGenerator.generateJpg
import nz.mega.documentscanner.utils.DocumentGenerator.generatePdf
import nz.mega.documentscanner.utils.DocumentUtils.deletePage
import nz.mega.documentscanner.utils.DocumentUtils.deletePages
import nz.mega.documentscanner.utils.FileUtils
import nz.mega.documentscanner.utils.LiveDataUtils.notifyObserver
import nz.mega.documentscanner.utils.PageUtils
import nz.mega.documentscanner.utils.PageUtils.deleteCropMat
import nz.mega.documentscanner.utils.PageUtils.deleteTransformImage
import org.opencv.core.MatOfPoint2f
import java.io.File

class DocumentScannerViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val document: MutableLiveData<Document> = MutableLiveData(Document())
    private val saveDestinations: MutableLiveData<Array<String>> = MutableLiveData()
    private val currentPagePosition: MutableLiveData<Int> = MutableLiveData(0)
    private val resultDocument: MutableLiveData<Uri> = MutableLiveData()

    fun getResultDocument(): LiveData<Uri> =
        resultDocument

    fun getDocumentTitle(): LiveData<String> =
        document.map { it.title }

    fun getDocumentQuality(): LiveData<Quality> =
        document.map { it.quality }

    fun getDocumentFileType(): LiveData<FileType> =
        document.map { it.fileType }

    fun getSaveDestinations(): LiveData<List<Pair<String, Boolean>>> =
        saveDestinations.map { destinations ->
            val currentSaveDestination = document.value?.saveDestination
            destinations.map { destination ->
                destination to (currentSaveDestination == destination)
            }
        }

    fun getSaveDestination(): String? =
        document.value?.saveDestination

    fun getDocumentPages(): LiveData<List<Page>> =
        document.map { it.pages.toList() }

    fun getCurrentPage(): LiveData<Page?> =
        currentPagePosition.map { document.value?.pages?.elementAtOrNull(it) }

    fun getCurrentPagePosition(): LiveData<Int> =
        currentPagePosition

    fun getPagesCount(): Int =
        document.value?.pages?.size ?: 0

    fun setCurrentPagePosition(position: Int) {
        currentPagePosition.value = position
    }

    fun setDocumentTitle(title: String) {
        if (document.value?.title == title) return

        document.value?.title = title.trim()
        document.notifyObserver()
    }

    fun setDocumentFileType(fileType: FileType) {
        if (document.value?.fileType == fileType) return

        document.value?.fileType = fileType
        document.notifyObserver()
    }

    fun setDocumentQuality(quality: Quality) {
        if (document.value?.quality == quality) return

        document.value?.quality = quality
        document.notifyObserver()
    }

    fun setDocumentSaveDestination(saveDestination: String) {
        if (document.value?.saveDestination == saveDestination) return

        document.value?.saveDestination = saveDestination
        document.notifyObserver()
    }

    fun setSaveDestinations(destinations: Array<String>) {
        document.value?.saveDestination = destinations.firstOrNull()
        saveDestinations.value = destinations
    }

    fun addPage(context: Context, bitmap: Bitmap): LiveData<Boolean> {
        val operationResult = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val originalImageFile = FileUtils.createImageFile(context, bitmap)
                var transformImageFile: File? = null
                var transformBitmap: Bitmap? = null

                val cropMat = ImageScanner.getCropPoints(bitmap)
                cropMat?.let {
                    transformBitmap = ImageScanner.getCroppedBitmap(bitmap, it)
                    transformImageFile = FileUtils.createImageFile(context, transformBitmap!!)
                }

                val page = Page(
                    originalImageUri = originalImageFile.toUri(),
                    transformImageUri = transformImageFile?.toUri(),
                    cropMat = cropMat
                )

                document.value?.pages?.add(page)

                bitmap.recycle()
                transformBitmap?.recycle()
                document.notifyObserver()
                updateDocumentFileType()
                operationResult.postValue(true)
            } catch (error: Exception) {
                Log.e(TAG, error.stackTraceToString())
                operationResult.postValue(false)
            }
        }

        return operationResult
    }

    fun rotatePage(context: Context, position: Int = currentPagePosition.value ?: 0) {
        viewModelScope.launch {
            document.value?.pages?.get(position)?.let { page ->
                val imageUri = page.transformImageUri ?: page.originalImageUri

                val transformBitmap = BitmapUtils.getBitmapFromUri(
                    imageUri = imageUri,
                    degreesToRotate = PageUtils.PAGE_ROTATION_DEGREES
                )

                val transformImageFile = FileUtils.createImageFile(context, transformBitmap)
                val updatedPage = page.copy(
                    transformImageUri = transformImageFile.toUri()
                )

                page.deleteTransformImage()
                transformBitmap.recycle()
                document.value?.pages?.set(position, updatedPage)
                document.notifyObserver()
            }
        }
    }

    fun cropPage(
        context: Context,
        cropMat: MatOfPoint2f,
        position: Int = currentPagePosition.value ?: 0
    ) {
        viewModelScope.launch {
            document.value?.pages?.get(position)?.let { page ->
                if (page.cropMat == cropMat) {
                    return@let
                }

                val originalBitmap = BitmapUtils.getBitmapFromUri(
                    imageUri = page.originalImageUri
                )

                val transformBitmap = ImageScanner.getCroppedBitmap(originalBitmap, cropMat)
                val transformImageFile = FileUtils.createImageFile(context, transformBitmap)

                val updatedPage = page.copy(
                    transformImageUri = transformImageFile.toUri(),
                    cropMat = cropMat
                )

                page.deleteCropMat()
                page.deleteTransformImage()
                transformBitmap.recycle()
                document.value?.pages?.set(position, updatedPage)
                document.notifyObserver()
            }
        }
    }

    fun deletePage(position: Int = currentPagePosition.value ?: 0) {
        viewModelScope.launch {
            document.value?.deletePage(position)
            document.notifyObserver()
        }
    }

    fun resetDocument() {
        viewModelScope.launch {
            document.value?.deletePages()
            document.notifyObserver()
        }
    }

    fun generateDocument(context: Context): MutableLiveData<Boolean> {
        val operationResult = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val document = requireNotNull(document.value)
                resultDocument.value = when (document.fileType) {
                    FileType.JPG -> document.generateJpg(context)
                    FileType.PDF -> document.generatePdf(context)
                }
                operationResult.postValue(true)
            } catch (error: Exception) {
                Log.e(TAG, error.stackTraceToString())
                operationResult.postValue(false)
            }
        }

        return operationResult
    }

    private fun updateDocumentFileType() {
        if (document.value?.pages?.size ?: 0 > 1) {
            document.value?.fileType = FileType.PDF
        }
    }
}
