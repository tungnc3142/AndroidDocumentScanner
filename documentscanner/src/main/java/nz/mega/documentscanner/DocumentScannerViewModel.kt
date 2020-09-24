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
import nz.mega.documentscanner.data.CropResult
import nz.mega.documentscanner.data.Document
import nz.mega.documentscanner.data.Document.FileType
import nz.mega.documentscanner.data.Document.Quality
import nz.mega.documentscanner.data.Image
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.utils.DocumentGenerator.generateJpg
import nz.mega.documentscanner.utils.DocumentGenerator.generatePdf
import nz.mega.documentscanner.utils.ImageScanner
import nz.mega.documentscanner.utils.ImageUtils
import nz.mega.documentscanner.utils.ImageUtils.crop
import nz.mega.documentscanner.utils.ImageUtils.deleteFile
import nz.mega.documentscanner.utils.ImageUtils.rotate
import nz.mega.documentscanner.utils.LiveDataUtils.notifyObserver

class DocumentScannerViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val document: MutableLiveData<Document> = MutableLiveData(Document())
    private val saveDestinations: MutableLiveData<Array<String>> = MutableLiveData()
    private val currentPagePosition: MutableLiveData<Int> = MutableLiveData(0)
    private val resultDocument: MutableLiveData<Uri> = MutableLiveData()
    private val imageScanner: ImageScanner by lazy { ImageScanner() }

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

    fun getDocumentPages(): LiveData<List<Page>> =
        document.map { it.pages.toList() }

    fun getCurrentPage(): LiveData<Page?> =
        currentPagePosition.map { document.value?.pages?.elementAtOrNull(it) }

    fun getCurrentPagePosition(): LiveData<Pair<Int, Int>> =
        currentPagePosition.map { it + 1 to (document.value?.pages?.size ?: 0) }

    fun setCurrentPagePosition(position: Int) {
        if (currentPagePosition.value != position) {
            currentPagePosition.value = position
        }
    }

    fun setDocumentTitle(title: String) {
        if (title.isBlank() || document.value?.title == title) return

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

    fun addPage(context: Context, originalBitmap: Bitmap, previewCropResult: CropResult?): LiveData<Boolean> {
        val addPageResult = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val originalImage = ImageUtils.createImageFromBitmap(context, originalBitmap)
                var cropPoints: List<PointF>? = null
                var croppedImage: Image? = null

//                TODO Override crop points with previewCropResult
//                if (previewCropResult != null) {
//                    cropPoints = previewCropResult.cropPoints
//                }

                val cropResult = imageScanner.getCroppedImage(originalBitmap, cropPoints)
                if (cropResult != null) {
                    cropPoints = cropResult.cropPoints
                    croppedImage = ImageUtils.createImageFromBitmap(context, cropResult.bitmap)
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
                addPageResult.postValue(true)
            } catch (error: Exception) {
                Log.e(TAG, error.stackTraceToString())
                addPageResult.postValue(false)
            }
        }

        return addPageResult
    }

    fun deleteCurrentPage() {
        val currentPosition = currentPagePosition.value ?: 0
        document.value?.pages?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    currentPage.originalImage.deleteFile()
                    currentPage.croppedImage?.deleteFile()

                    document.value?.pages?.remove(currentPage)

                    updateDocumentFileType()
                    document.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun rotateCurrentPage(context: Context) {
        val currentPosition = currentPagePosition.value ?: 0
        document.value?.pages?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    val image = currentPage.getImageToPrint()
                    val rotatedImage = image.rotate(context)
                    image.deleteFile()

                    val updatedPage = currentPage.copy(
                        croppedImage = rotatedImage
                    )

                    document.value?.pages?.set(currentPosition, updatedPage)
                    document.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun cropCurrentPage(context: Context, points: List<PointF>) {
        val currentPosition = currentPagePosition.value ?: 0
        document.value?.pages?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    val image = currentPage.getImageToPrint()
                    val croppedImage = image.crop(context, imageScanner, points)
                    image.deleteFile()

                    val updatedPage = currentPage.copy(
                        croppedImage = croppedImage
                    )

                    document.value?.pages?.set(currentPosition, updatedPage)
                    document.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    private fun updateDocumentFileType() {
        if (document.value?.pages?.size ?: 0 > 1) {
            document.value?.fileType = FileType.PDF
        }
    }

    fun generateDocument(context: Context) {
        viewModelScope.launch {
            try {
                val currentDocument = requireNotNull(document.value)
                val generatedDocumentUri = when (currentDocument.fileType) {
                    FileType.JPG -> currentDocument.generateJpg(context)
                    else -> currentDocument.generatePdf(context)
                }

                resultDocument.value = generatedDocumentUri
            } catch (error: Exception) {
                Log.e(TAG, error.stackTraceToString())
            }
        }
    }
}
