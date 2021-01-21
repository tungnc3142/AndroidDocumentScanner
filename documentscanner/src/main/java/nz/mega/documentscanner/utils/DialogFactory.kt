package nz.mega.documentscanner.utils

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nz.mega.documentscanner.R

object DialogFactory {

    fun createDiscardScanDialog(context: Context, callback: () -> Unit): MaterialAlertDialogBuilder =
        createDialog(context, callback, R.string.scan_dialog_discard_title, R.string.scan_dialog_discard_body)

    fun createDiscardScansDialog(context: Context, callback: () -> Unit): MaterialAlertDialogBuilder =
        createDialog(context, callback, R.string.scan_dialog_discard_all_title, R.string.scan_dialog_discard_all_body)

    fun createDeleteCurrentScanDialog(context: Context, callback: () -> Unit): MaterialAlertDialogBuilder =
        createDialog(context, callback, R.string.scan_dialog_delete_body, null)

    private fun createDialog(
        context: Context,
        callback: () -> Unit,
        @StringRes titleRes: Int,
        @StringRes bodyRes: Int?
    ): MaterialAlertDialogBuilder =
        MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int -> callback.invoke() }
            .apply { bodyRes?.let { setMessage(it) } }
}
