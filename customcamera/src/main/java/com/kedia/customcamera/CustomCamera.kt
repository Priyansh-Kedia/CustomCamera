package com.kedia.customcamera

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.android.synthetic.main.custom_camera.view.*
import java.io.File
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CustomCamera : FrameLayout, LifecycleOwner {

    @LayoutRes
    private var mainLayoutId = 0

    private lateinit var surfaceHolder: SurfaceHolder


    private lateinit var jpegCallback: Camera.PictureCallback
    private val TAG = "TAG!!!!"

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var outputDirectory: File

    private lateinit var camera: Camera

    private val imageArrayList: MutableList<Bitmap?> = mutableListOf()
//    private val customCameraAdapter by lazy { CustomImageAdapter(context, mutableListOf()) }
//    private val linearLayoutManager by lazy { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }

    private var imageCapture: ImageCapture? = null
//

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(surfaceView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

//            val imageAnalyzer = ImageAnalysis.Builder()
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
//                        Log.d(TAG, "Average luminosity: $luma")
//                    })
//                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = kotlin.math.max(width, height).toDouble() / kotlin.math.min(
            width,
            height
        )
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
        initLayout(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomCamera)
        try {
            mainLayoutId = R.layout.custom_camera
        } finally {
            typedArray.recycle()
        }
    }

    private fun initLayout(attrs: AttributeSet?) {
        if (isInEditMode) {
            return
        }

        val view = LayoutInflater.from(context).inflate(mainLayoutId, this)

        startCamera()

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        captureImage.setOnClickListener {
            takePhoto()
            Log.d(TAG, "clicked")
        }

    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        Log.d(TAG, "returned")

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()
            ).format(System.currentTimeMillis()) + ".jpg")

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        Log.d(TAG, outputOptions.toString() + "    +++   " + photoFile.absoluteFile)
        // Set up image capture listener, which is triggered after photo has
        // been taken
//        imageCapture.takePicture(
//            outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    val savedUri = Uri.fromFile(photoFile)
//                    val msg = "Photo capture succeeded: $savedUri"
//
//                    Log.d(TAG, msg)
//                }
//            })
        imageCapture.takePicture(ContextCompat.getMainExecutor(context),object : ImageCapture.OnImageCapturedCallback(){
            override fun onCaptureSuccess(image: ImageProxy) {
                // Use the image, then make sure to close it.
                Log.d(TAG, image.toString())
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
                capturedImage.apply {
                    setImageBitmap(rotateBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)))
                    visibility = View.VISIBLE
                }

            }

            override fun onError(exception: ImageCaptureException) {
                val errorType = exception.getImageCaptureError()

            }
        })
    }

    private fun rotateBitmap(decodeByteArray: Bitmap?): Bitmap? {
        val width = decodeByteArray?.width
        val height = decodeByteArray?.height

        val matrix = Matrix()
        matrix.postRotate(90f)
        val rotatedImage = Bitmap.createBitmap(decodeByteArray!!, 0, 0, width!!, height!!, matrix, true)
        decodeByteArray.recycle()
        return rotatedImage
    }

    private fun getOutputDirectory(): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "somename").apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun getLifecycle(): Lifecycle {
        val lifecycleRegistry = LifecycleRegistry(this);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        return lifecycleRegistry
    }

}