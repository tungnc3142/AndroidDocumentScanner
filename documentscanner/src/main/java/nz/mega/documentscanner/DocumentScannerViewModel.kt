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
import nz.mega.documentscanner.data.ScanDocument
import nz.mega.documentscanner.utils.FileUtils
import nz.mega.documentscanner.utils.ImageScanner
import nz.mega.documentscanner.utils.LiveDataUtils.notifyObserver
import java.util.Calendar

class DocumentScannerViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    var destinations: MutableLiveData<Array<String>?> = MutableLiveData(arrayOf("Cloud Drive", "Chat")) // TODO Remove hardcoded destinations

    private val documents: MutableLiveData<MutableList<ScanDocument>> = MutableLiveData(mutableListOf())
    private val currentDocumentPosition: MutableLiveData<Int> = MutableLiveData(0)
    private val imageScanner: ImageScanner by lazy { ImageScanner() }

    fun getDocuments(): LiveData<List<ScanDocument>> =
        documents.map { it.toList() }

    fun getCurrentDocument(): LiveData<ScanDocument?> =
        currentDocumentPosition.map { documents.value?.elementAtOrNull(it) }

    fun addDocument(context: Context, imageUri: Uri): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        viewModelScope.launch {
            try {
                val scannerResult = imageScanner.processImage(context, imageUri)
                val title = String.format(FileUtils.DOCUMENT_NAME_FORMAT, Calendar.getInstance())
                val document = ScanDocument(
                    title = title,
                    width = scannerResult.imageWidth,
                    height = scannerResult.imageHeight,
                    originalImageUri = imageUri,
                    croppedImageUri = scannerResult.imageUri,
                    cropPoints = scannerResult.points
                )

                documents.value?.add(document)
                documents.notifyObserver()
                result.postValue(true)
            } catch (error: Exception) {
                Log.e(TAG, "Error: ${error.stackTraceToString()}")
                result.postValue(false)
            }
        }

        return result
    }

    fun deleteCurrentDocument() {
        val currentPosition = currentDocumentPosition.value ?: 0
        documents.value?.get(currentPosition)?.let { currentDocument ->
            viewModelScope.launch {
                try {
                    currentDocument.deleteFiles()

                    documents.value?.remove(currentDocument)
                    documents.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun rotateCurrentDocument() {
        val currentPosition = currentDocumentPosition.value ?: 0
        documents.value?.get(currentPosition)?.let { currentDocument ->
            viewModelScope.launch {
                try {
                    currentDocument.rotateFiles()

                    val newRotation = if (currentDocument.rotation < 360) {
                        currentDocument.rotation + 90
                    } else {
                        0
                    }

                    val newDocument = currentDocument.copy(rotation = newRotation)

                    documents.value?.set(currentPosition, newDocument)
                    documents.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun cropCurrentDocument(context: Context, points: List<PointF>) {
        val currentPosition = currentDocumentPosition.value ?: 0
        documents.value?.get(currentPosition)?.let { currentDocument ->
            viewModelScope.launch {
                try {
                    currentDocument.deleteCroppedFile()

                    val scannerResult = imageScanner.processImage(context, currentDocument.originalImageUri, points)
                    val newDocument = currentDocument.copy(
                        croppedImageUri = scannerResult.imageUri,
                        cropPoints = scannerResult.points
                    )

                    documents.value?.set(currentPosition, newDocument)
                    documents.notifyObserver()
                } catch (error: Exception) {
                    Log.e(TAG, error.stackTraceToString())
                }
            }
        }
    }

    fun getCurrentDocumentPosition(): LiveData<Pair<Int, Int>> =
        currentDocumentPosition.map { it + 1 to (documents.value?.size ?: 0) }

    fun setCurrentDocumentPosition(position: Int) {
        if (currentDocumentPosition.value != position) {
            currentDocumentPosition.value = position
        }
    }
}
