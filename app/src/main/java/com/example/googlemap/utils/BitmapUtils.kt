package com.example.googlemap.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import java.net.HttpURLConnection
import java.net.URL

class BitmapUtils(private val context: Context) {

    fun getBitmapFromUrl(imageUrl: String?): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            getCircleBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getBitmapFromUrlWithoutCircle(imageUrl: String?): Bitmap? {
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun getCircleBitmap(bitmap: Bitmap): Bitmap? {
        val output: Bitmap
        val srcRect: Rect
        val dstRect: Rect
        val r: Float
        val width = bitmap.width
        val height = bitmap.height
        if (width > height) {
            output = Bitmap.createBitmap(height, height, Bitmap.Config.ARGB_8888)
            val left = (width - height) / 2
            val right = left + height
            srcRect = Rect(left, 0, right, height)
            dstRect = Rect(0, 0, height, height)
            r = (height shr 1).toFloat()
        } else {
            output = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
            val top = (height - width) / 2
            val bottom = top + width
            srcRect = Rect(0, top, width, bottom)
            dstRect = Rect(0, 0, width, width)
            r = (width shr 1).toFloat()
        }
        val canvas = Canvas(output)
        val color = -0xbdbdbe
        val paint = Paint()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(r, r, r, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
        bitmap.recycle()
        return output
    }

     fun getActionText(@StringRes stringRes: Int, @ColorRes colorRes: Int): Spannable {
        val spannable: Spannable = SpannableString(context.getText(stringRes))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            // This will only work for cases where the Notification.Builder has a fullscreen intent set
            // Notification.Builder that does not have a full screen intent will take the color of the
            // app and the following leads to a no-op.
            spannable.setSpan(
                ForegroundColorSpan(context.getColor(colorRes)), 0, spannable.length, 0
            )
        }
        return spannable
    }
}