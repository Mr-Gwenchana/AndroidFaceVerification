package com.bachelor.projectiii.interfaces

import android.graphics.Bitmap
import android.net.Uri

interface IFaceStorageManager {
    fun saveCroppedFace(bitmap: Bitmap, trackingId: Int?): Uri?
}
