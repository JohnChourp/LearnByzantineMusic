package com.johnchourp.learnbyzantinemusic.editor.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PngExporter(private val context: Context) {
    fun export(view: View): File {
        val bitmap = Bitmap.createBitmap(
            view.measuredWidth.coerceAtLeast(1),
            view.measuredHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val outputDir = resolveExportsDirectory(context)
        outputDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(outputDir, "composition_$timestamp.png")

        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        return file
    }

    companion object {
        private const val EXPORTS_DIR_NAME = "exports"

        fun resolveExportsDirectory(context: Context): File {
            return context.getExternalFilesDir(EXPORTS_DIR_NAME) ?: context.filesDir
        }
    }
}
