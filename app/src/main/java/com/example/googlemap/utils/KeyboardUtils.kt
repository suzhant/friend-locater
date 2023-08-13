package com.example.googlemap.utils

import android.content.Context
import android.view.inputmethod.InputMethodManager
import com.google.android.material.textfield.TextInputEditText


class KeyboardUtils {

    companion object{
        fun showSoftKeyboard(editText: TextInputEditText,context: Context){
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        fun hideSoftKeyboard(editText: TextInputEditText,context: Context){
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(editText.windowToken, 0)
        }
    }

}