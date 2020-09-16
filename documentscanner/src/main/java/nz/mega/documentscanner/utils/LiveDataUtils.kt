package nz.mega.documentscanner.utils

import androidx.lifecycle.MutableLiveData

object LiveDataUtils {

    fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }
}
