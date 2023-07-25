package com.example.googlemap.utils

import android.app.Dialog
import android.content.Context
import com.example.googlemap.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

 object ProgressHelper{
     fun buildProgressDialog(context: Context) : Dialog{
        val dialog = MaterialAlertDialogBuilder(context,R.style.ProgressDialogTheme)
        dialog.setView(R.layout.progress_bar)
        dialog.setCancelable(false)
        return dialog.create()
    }

}

