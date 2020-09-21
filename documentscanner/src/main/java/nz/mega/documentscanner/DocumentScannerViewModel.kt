package nz.mega.documentscanner

import android.content.Context
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
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.utils.ImageScanner
import nz.mega.documentscanner.utils.LiveDataUtils.notifyObserver

class DocumentScannerViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val document: MutableLiveData<Document> = MutableLiveData(Document())
    private val saveDestinations: MutableLiveData<Array<String>> = MutableLiveData()
    private val currentPagePosition: MutableLiveData<Int> = MutableLiveData(0)
    private val imageScanner: ImageScanner by lazy { ImageScanner() }

    fun getDocumentTitle(): LiveData<String> =
        document.map { it.title }

    fun getDocumentQuality(): LiveData<Document.Quality> =
        document.map { it.quality }

    fun getDocumentFileType(): LiveData<Document.FileType> =
        document.map { it.fileType }

    fun getSaveDestinations(): LiveData<List<Pair<String, Boolean>>> =
        saveDestinations.map { destinations ->
            val currentSaveDestination = document.value?.saveDestination
            destinations.map { destination ->
                destination to (currentSaveDestination == destination)
            }
        }

    fun getPages(): LiveData<List<Page>> =
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

    fun setDocumentFileType(fileType: Document.FileType) {
        if (document.value?.fileType == fileType) return

        document.value?.fileType = fileType
        document.notifyObserver()
    }

    fun setDocumentQuality(quality: Document.Quality) {
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

    fun addPage(context: Context, imageUri: Uri): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val scannerResult = imageScanner.processImage(context, imageUri)
                val page = Page(
                    width = scannerResult.imageWidth,
                    height = scannerResult.imageHeight,
                    originalImageUri = imageUri,
                    croppedImageUri = scannerResult.imageUri,
                    cropPoints = scannerResult.points
                )

                document.value?.pages?.add(page)
                document.notifyObserver()
                result.postValue(true)
            } catch (error: Exception) {
                Log.e(TAG, "Error: ${error.stackTraceToString()}")
                result.postValue(false)
            }
        }

        return result
    }

    fun deleteCurrentPage() {
        val currentPosition = currentPagePosition.value ?: 0
        document.value?.pages?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    currentPage.deleteFiles()

                    document.value?.pages?.remove(currentPage)
                    document.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun rotateCurrentPage() {
        val currentPosition = currentPagePosition.value ?: 0
        document.value?.pages?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    currentPage.rotateFiles()

                    val newRotation = if (currentPage.rotation < 360) {
                        currentPage.rotation + 90
                    } else {
                        0
                    }

                    val newPage = currentPage.copy(rotation = newRotation)

                    document.value?.pages?.set(currentPosition, newPage)
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
                    currentPage.deleteCroppedFile()

                    val scannerResult = imageScanner.processImage(context, currentPage.originalImageUri, points)
                    val newPage = currentPage.copy(
                        croppedImageUri = scannerResult.imageUri,
                        cropPoints = scannerResult.points
                    )

                    document.value?.pages?.set(currentPosition, newPage)
                    document.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun generateDocument() {
        // TODO
    }
}
