package nz.mega.documentscanner

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import nz.mega.documentscanner.data.ScanDocument
import nz.mega.documentscanner.utils.FileUtils
import nz.mega.documentscanner.utils.ImageScanner
import nz.mega.documentscanner.utils.LiveDataUtils.notifyObserver
import java.util.*

class DocumentScannerViewModel : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val documents: MutableLiveData<MutableList<ScanDocument>> = MutableLiveData(mutableListOf())
    private val currentDocumentPosition: MutableLiveData<Int> = MutableLiveData(0)
    private val imageScanner: ImageScanner by lazy { ImageScanner() }

    fun getDocuments(): LiveData<List<ScanDocument>> =
        documents.map { it.toList() }

    fun getCurrentDocument(): LiveData<ScanDocument?> =
        currentDocumentPosition.map { documents.value?.elementAtOrNull(it) }

    @SuppressLint("CheckResult")
    fun addDocument(context: Context, imageUri: Uri): LiveData<Boolean> {
        val result = MutableLiveData<Boolean>()

        imageScanner.processImage(context, imageUri)
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { resultImage ->
                    val title = String.format(FileUtils.DOCUMENT_NAME_FORMAT, Calendar.getInstance())
                    val document = ScanDocument(
                        title = title,
                        width = resultImage.imageWidth,
                        height = resultImage.imageHeight,
                        originalImageUri = imageUri,
                        croppedImageUri = resultImage.imageUri,
                        cropPoints = resultImage.points
                    )

                    documents.value?.add(document)
                    documents.notifyObserver()
                    result.postValue(true)
                },
                { error ->
                    Log.e(TAG, "Error: ${error.stackTraceToString()}")
                    result.postValue(false)
                }
            )

        return result
    }

    fun deleteCurrentDocument() {
        val currentPosition = currentDocumentPosition.value ?: 0
        documents.value?.get(currentPosition)?.let { currentDocument ->
            currentDocument.deleteFiles()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        documents.value?.remove(currentDocument)
                        documents.notifyObserver()
                    },
                    { error ->
                        Log.e(TAG, error.stackTraceToString())
                    }
                )
        }
    }

    fun rotateCurrentDocument() {
        val currentPosition = currentDocumentPosition.value ?: 0
        documents.value?.get(currentPosition)?.let { currentDocument ->
            currentDocument.rotateFiles()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        val newRotation = if (currentDocument.rotation < 360) {
                            currentDocument.rotation + 90
                        } else {
                            0
                        }

                        val newDocument = currentDocument.copy(rotation = newRotation)

                        documents.value?.set(currentPosition, newDocument)
                        documents.notifyObserver()
                    },
                    { error ->
                        Log.e(TAG, error.message.toString())
                    }
                )
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
