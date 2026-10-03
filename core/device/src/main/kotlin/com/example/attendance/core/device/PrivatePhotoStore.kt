package com.example.attendance.core.device

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.example.attendance.core.domain.DomainException
import com.example.attendance.core.domain.PhotoStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PrivatePhotoStore(private val context: Context) : PhotoStore {
    private val directory = File(context.filesDir, "photos").also { it.mkdirs() }
    override fun path(key: String): String {
        require(key.matches(Regex("[a-f0-9-]+\\.jpg"))) { "Invalid image key" }
        return File(directory, key).absolutePath
    }

    override suspend fun importImage(uri: String): String = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(source)
            ?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw DomainException("This image cannot be opened.")
        var sample = 1
        while (maxOf(options.outWidth, options.outHeight) / sample > 1600) sample *= 2
        val bitmap = context.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(
                it,
                null,
                BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw DomainException("Unable to read photo.")
        val exif = context.contentResolver.openInputStream(source)?.use { ExifInterface(it) }
        val matrix = Matrix().apply {
            if (exif?.isFlipped == true) postScale(
                -1f,
                1f
            ); postRotate((exif?.rotationDegrees ?: 0).toFloat())
        }
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val key = UUID.randomUUID().toString() + ".jpg"
        try {
            File(path(key)).outputStream()
                .use { oriented.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        } finally {
            if (oriented !== bitmap) oriented.recycle(); bitmap.recycle()
        }
        key
    }

    override suspend fun delete(key: String) =
        withContext(Dispatchers.IO) { File(path(key)).delete(); Unit }
}
