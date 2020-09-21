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
import nz.mega.documentscanner.data.Page
import nz.mega.documentscanner.utils.FileUtils
import nz.mega.documentscanner.utils.ImageScanner
import nz.mega.documentscanner.utils.LiveDataUtils.notifyObserver
import java.util.Calendar

class DocumentScannerViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    var saveDestinations: MutableLiveData<Array<String>?> = MutableLiveData(null)

    private val pages: MutableLiveData<MutableList<Page>> = MutableLiveData(mutableListOf())
    private val currentPagePosition: MutableLiveData<Int> = MutableLiveData(0)
    private val imageScanner: ImageScanner by lazy { ImageScanner() }

    fun getPages(): LiveData<List<Page>> =
        pages.map { it.toList() }

    fun getCurrentPage(): LiveData<Page?> =
        currentPagePosition.map { pages.value?.elementAtOrNull(it) }

    fun addPage(context: Context, imageUri: Uri): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val scannerResult = imageScanner.processImage(context, imageUri)
                val title = String.format(FileUtils.FILE_NAME_FORMAT, Calendar.getInstance())
                val page = Page(
                    title = title,
                    width = scannerResult.imageWidth,
                    height = scannerResult.imageHeight,
                    originalImageUri = imageUri,
                    croppedImageUri = scannerResult.imageUri,
                    cropPoints = scannerResult.points
                )

                pages.value?.add(page)
                pages.notifyObserver()
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
        pages.value?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    currentPage.deleteFiles()

                    pages.value?.remove(currentPage)
                    pages.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun rotateCurrentPage() {
        val currentPosition = currentPagePosition.value ?: 0
        pages.value?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    currentPage.rotateFiles()

                    val newRotation = if (currentPage.rotation < 360) {
                        currentPage.rotation + 90
                    } else {
                        0
                    }

                    val newPage = currentPage.copy(rotation = newRotation)

                    pages.value?.set(currentPosition, newPage)
                    pages.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun cropCurrentPage(context: Context, points: List<PointF>) {
        val currentPosition = currentPagePosition.value ?: 0
        pages.value?.get(currentPosition)?.let { currentPage ->
            viewModelScope.launch {
                try {
                    currentPage.deleteCroppedFile()

                    val scannerResult = imageScanner.processImage(context, currentPage.originalImageUri, points)
                    val newPage = currentPage.copy(
                        croppedImageUri = scannerResult.imageUri,
                        cropPoints = scannerResult.points
                    )

                    pages.value?.set(currentPosition, newPage)
                    pages.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun getCurrentPagePosition(): LiveData<Pair<Int, Int>> =
        currentPagePosition.map { it + 1 to (pages.value?.size ?: 0) }

    fun setCurrentPagePosition(position: Int) {
        if (currentPagePosition.value != position) {
            currentPagePosition.value = position
        }
    }
}
