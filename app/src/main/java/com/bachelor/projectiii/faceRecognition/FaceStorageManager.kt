package com.bachelor.projectiii.faceRecognition

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.bachelor.projectiii.interfaces.IFaceStorageManager

class FaceStorageManager(
    private val context: Context,
    private val quality: Int = 80
) : IFaceStorageManager {

    override fun saveCroppedFace(bitmap: Bitmap, trackingId: Int?): Uri? {
        val fileName = "face_${System.currentTimeMillis()}_id${trackingId ?: 0}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProjectIII_Faces")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            Log.d(TAG, "Đã lưu khuôn mặt vào Album: $fileName tại $uri")
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Log.e(TAG, "Lỗi lưu khuôn mặt vào Album", e)
            null
        }
    }

    companion object {
        private const val TAG = "FaceStorageManager"
    }
}
