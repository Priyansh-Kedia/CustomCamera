package com.kedia.customcamera.utils

import android.graphics.Bitmap
import android.net.Uri
import androidx.camera.core.ImageProxy

interface CustomMultiple {
    fun onConfirmImagesClicked(imageArrayList: MutableList<Uri>)
    fun onCameraFrameReceived(bitmap: Bitmap)
}
