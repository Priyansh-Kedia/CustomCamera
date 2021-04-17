package com.kedia.customcamera.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Point
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.Display
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class SmartSize(width: Int, height: Int) {
    var size = Size(width, height)
    var long = max(size.width, size.height)
    var short = min(size.width, size.height)
    override fun toString() = "SmartSize(${long}x${short})"
}

/** Standard High Definition size for pictures and video */
val SIZE_1080P: SmartSize = SmartSize(1920, 1080)

/** Returns a [SmartSize] object for the given [Display] */
fun getDisplaySmartSize(display: Display): SmartSize {
    val outPoint = Point()
    display.getRealSize(outPoint)
    return SmartSize(outPoint.x, outPoint.y)
}

/**
 * Returns the largest available PREVIEW size. For more information, see:
 * https://d.android.com/reference/android/hardware/camera2/CameraDevice and
 * https://developer.android.com/reference/android/hardware/camera2/params/StreamConfigurationMap
 */
fun <T>getPreviewOutputSize(
    display: Display,
    characteristics: CameraCharacteristics,
    targetClass: Class<T>,
    format: Int? = null
): Size {

    // Find which is smaller: screen or 1080p
    val screenSize = getDisplaySmartSize(display)
    val hdScreen = screenSize.long >= SIZE_1080P.long || screenSize.short >= SIZE_1080P.short
    val maxSize = if (hdScreen) SIZE_1080P else screenSize

    // If image format is provided, use it to determine supported sizes; else use target class
    val config = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
    if (format == null)
        assert(StreamConfigurationMap.isOutputSupportedFor(targetClass))
    else
        assert(config.isOutputSupportedFor(format))
    val allSizes = if (format == null)
        config.getOutputSizes(targetClass) else config.getOutputSizes(format)

    // Get available sizes and sort them by area from largest to smallest
    val validSizes = allSizes
        .sortedWith(compareBy { it.height * it.width })
        .map { SmartSize(it.width, it.height) }.reversed()

    // Then, get the largest output size that is smaller or equal than our max size
    return validSizes.first { it.long <= maxSize.long && it.short <= maxSize.short }.size
}

fun View.makeVisible() {
    this.visibility = View.VISIBLE
}

fun View.makeGone() {
    this.visibility = View.GONE
}

fun log(message: String) {
    Log.d("CCMultiple", message)
}

fun logV(message: String) {
    Log.v("CCMultiple", message)
}

fun logE(message: String) {
    Log.e("CCMultiple", message)
}

fun getBitmap(context: Context, uri: Uri): Bitmap? {
    try {
        val bitmap: Bitmap? = generateBitmap(context, uri)
        context.contentResolver.delete(uri, null, null)
        return bitmap
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

@Throws(IOException::class)
fun generateBitmap(context: Context, uri: Uri?): Bitmap? {
    return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
}

fun getUri(context: Context, bitmap: Bitmap): Uri? {
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(
        context.contentResolver,
        bitmap,
        "IMG_" + System.currentTimeMillis(),
        null
    )
   // bitmap.recycle()
    return Uri.parse(path)
}

private fun saveImage(
    context: Context,
    bitmap: Bitmap,
    folderName: String = "CCImages",
    fileName: String = "CC${System.currentTimeMillis()}Multiple"
): Uri? {
    val pname = context.packageName
    try {
        val path = File("/storage/emulated/0/Android/data/$pname/files$folderName")
        if (!path.exists()) {
            path.mkdirs()
        }
        val outFile = File(path, "$fileName.jpeg")
        val out = FileOutputStream(outFile)
        val a = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.close()
        return outFile.toUri()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return Uri.EMPTY
}

fun rotateBitmap(decodeByteArray: Bitmap?, lensFacing: CameraSelector): Bitmap? {
    val width = decodeByteArray?.width
    val height = decodeByteArray?.height

    val matrix = Matrix()
    if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
        matrix.postRotate(90f)
        matrix.preScale(-1f,1f)
    }
    else {
        matrix.postRotate(90f)
    }
    val rotatedImage = Bitmap.createBitmap(decodeByteArray!!, 0, 0, width!!, height!!, matrix, true)
    decodeByteArray.recycle()
    return rotatedImage
}