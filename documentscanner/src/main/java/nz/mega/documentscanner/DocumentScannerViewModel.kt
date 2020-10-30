package nz.mega.documentscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.data.Document.FileType
import nz.mega.documentscanner.data.Document.Quality
import nz.mega.documentscanner.data.Image
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.openCV.ImageScanner
import nz.mega.documentscanner.utils.BitmapUtils
import nz.mega.documentscanner.utils.DocumentGenerator.generateJpg
import nz.mega.documentscanner.utils.DocumentGenerator.generatePdf
import nz.mega.documentscanner.utils.DocumentUtils.deleteAllPages
import nz.mega.documentscanner.utils.DocumentUtils.deletePage
import nz.mega.documentscanner.utils.ImageUtils.deleteFile
import nz.mega.documentscanner.utils.ImageUtils.toImage
import nz.mega.documentscanner.utils.LiveDataUtils.notifyObserver
import nz.mega.documentscanner.utils.PageUtils.rotate

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

    fun getCurrentPagePosition(): Int =
        currentPagePosition.value ?: 0

    fun getPagesCount(): Int =
        document.value?.pages?.size ?: 0

    fun setCurrentPagePosition(position: Int) {
        if (currentPagePosition.value != position) {
            currentPagePosition.value = position
        }
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

    fun addPage(context: Context, originalBitmap: Bitmap): LiveData<Boolean> {
        val operationResult = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val originalImage = originalBitmap.toImage(context)
                var cropPoints: List<PointF>? = null
                var croppedImage: Image? = null

                val cropResult = ImageScanner.getCroppedImage(originalBitmap, cropPoints)
                if (cropResult != null) {
                    cropPoints = cropResult.cropPoints
                    croppedImage = cropResult.bitmap.toImage(context)
                }

                val page = Page(
                    originalImage = originalImage,
                    croppedImage = croppedImage,
                    cropPoints = cropPoints
                )

                document.value?.pages?.add(page)

                originalBitmap.recycle()
                cropResult?.bitmap?.recycle()

                updateDocumentFileType()
                document.notifyObserver()
                operationResult.postValue(true)
            } catch (error: Exception) {
                Log.e(TAG, error.stackTraceToString())
                operationResult.postValue(false)
            }
        }

        return operationResult
    }

    fun rotateCurrentPage(context: Context): LiveData<Boolean> {
        val operationResult = MutableLiveData<Boolean>()

        val currentPosition = currentPagePosition.value ?: 0
        document.value?.pages?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    val updatedPage = currentPage.rotate(context)
                    document.value?.pages?.set(currentPosition, updatedPage)

                    document.notifyObserver()
                    operationResult.postValue(true)
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                    operationResult.postValue(false)
                }
            }
        }

        return operationResult
    }

    fun cropCurrentPage(context: Context, cropPoints: List<PointF>): LiveData<Boolean> {
        val operationResult = MutableLiveData<Boolean>()

        val currentPosition = currentPagePosition.value ?: 0
        document.value?.pages?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    if (cropPoints == currentPage.cropPoints) return@launch

                    val image = currentPage.originalImage
                    val originalImageBitmap = BitmapUtils.getBitmapFromUri(context, image.imageUri)
                    val cropResult = ImageScanner.getCroppedImage(originalImageBitmap, cropPoints)
                    val croppedBitmap = cropResult!!.bitmap
                    val croppedImage = croppedBitmap.toImage(context)

                    currentPage.croppedImage?.deleteFile()

                    val updatedPage = currentPage.copy(
                        croppedImage = croppedImage,
                        cropPoints = cropResult.cropPoints
                    )

                    document.value?.pages?.set(currentPosition, updatedPage)

                    originalImageBitmap.recycle()
                    croppedBitmap.recycle()
                    document.notifyObserver()

                    operationResult.postValue(true)
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                    operationResult.postValue(false)
                }
            }
        }

        return operationResult
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

    fun deleteCurrentPage() {
        viewModelScope.launch {
            val currentPosition = currentPagePosition.value ?: 0
            document.value?.deletePage(currentPosition)
            document.notifyObserver()
        }
    }

    fun deleteAllPages() {
        viewModelScope.launch {
            document.value?.deleteAllPages()
            document.notifyObserver()
        }
    }

    private fun updateDocumentFileType() {
        if (document.value?.pages?.size ?: 0 > 1) {
            document.value?.fileType = FileType.PDF
        }
    }
}
