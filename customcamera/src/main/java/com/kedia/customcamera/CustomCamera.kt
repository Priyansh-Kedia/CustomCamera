package com.kedia.customcamera

import android.Manifest
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.LayoutRes
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColor
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private var snapButtonColor: Int = context.resources.getColor(R.color.cardview_light_background)
    private var snapButtonSelectedColor: Int = context.resources.getColor(R.color.cardview_light_background)
    private var showSnapButton: Boolean = false

    private lateinit var surfaceHolder: SurfaceHolder


    private lateinit var jpegCallback: Camera.PictureCallback
    private val TAG = "TAG!!!!"

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var outputDirectory: File

    private lateinit var camera: Camera

    private val imageArrayList: MutableList<Bitmap?> = mutableListOf()
    private val customCameraAdapter by lazy { CustomImageAdapter(context, mutableListOf()) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }

    private var imageCapture: ImageCapture? = null
//

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
        initLayout()
    }

    private fun init(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomCamera)
        try {
            mainLayoutId = R.layout.custom_camera
            showSnapButton = typedArray.getBoolean(R.styleable.CustomCamera_showSnapButton, false)
            snapButtonColor = typedArray.getColor(R.styleable.CustomCamera_snapButtonColor, context.resources.getColor(R.color.cardview_light_background))
            snapButtonSelectedColor = typedArray.getColor(R.styleable.CustomCamera_snapButtonSelectedColor, Color.parseColor("#CB0000"))
        } finally {
            typedArray.recycle()
        }
    }

    private fun initLayout() {
        if (isInEditMode) {
            return
        }

        val view = LayoutInflater.from(context).inflate(mainLayoutId, this)

        startCamera()

        imageRecyclerView.apply {
            adapter = customCameraAdapter
            layoutManager = linearLayoutManager
        }



        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()

        captureImage.apply {
            isVisible = showSnapButton
            backgroundTintList = ColorStateList.valueOf(snapButtonColor)
        }

//        captureImage.setOnClickListener {
//            takePhoto()
//            Log.d(TAG, "clicked")
//        }

        captureImage.setOnTouchListener { view, motionEvent ->
            when(motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.backgroundTintList = ColorStateList.valueOf(snapButtonSelectedColor)
                    view.isSelected = true
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP -> {
                    view.isSelected = false
                    view.backgroundTintList = ColorStateList.valueOf(snapButtonColor)
                    takePhoto()
                    return@setOnTouchListener true
                }
                else -> {return@setOnTouchListener true}
            }
        }


    }

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
        if (abs(previewRatio - RATIO_4_3_VALUE) <= kotlin.math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
//        val photoFile = File(
//            outputDirectory,
//            SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault()
//            ).format(System.currentTimeMillis()) + ".jpg")
//
//        // Create output options object which contains file + metadata
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//        Log.d(TAG, outputOptions.toString() + "    +++   " + photoFile.absoluteFile)
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
        imageCapture.takePicture(ContextCompat.getMainExecutor(context),object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // Use the image, then make sure to close it.
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
                val rotatedBitmap = rotateBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                capturedImage.apply {
                    setImageBitmap(rotatedBitmap)
                }

                val atTop = !imageRecyclerView.canScrollVertically(-1)


                customCameraAdapter.addData(rotatedBitmap)

                if (atTop) {
                    imageRecyclerView.scrollToPosition(0)
                }
                recaptureImage.isVisible = capturedImage.isVisible
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                val errorType = exception.imageCaptureError

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