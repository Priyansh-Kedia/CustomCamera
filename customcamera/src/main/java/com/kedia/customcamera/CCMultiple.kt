package com.kedia.customcamera

import android.Manifest
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.common.util.concurrent.ListenableFuture
import com.kedia.customcamera.utils.makeGone
import com.kedia.customcamera.utils.makeVisible
import kotlinx.android.synthetic.main.custom_camera.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Math.abs
import java.util.concurrent.ExecutorService


class CCMultiple : FrameLayout, LifecycleOwner, LifecycleEventObserver {

    @LayoutRes
    private var mainLayoutId = 0

    private var snapButtonColor: Int = context.resources.getColor(R.color.cardview_light_background)
    private var snapButtonSelectedColor: Int = context.resources.getColor(R.color.cardview_light_background)
    private var showSnapButton: Boolean = false
    private var showPreviewScreen: Boolean = false

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA

    private lateinit var listener: CustomCamera

    private lateinit var preview: Preview
    private lateinit var gestureDetector: GestureDetector

    private lateinit var surfaceHolder: SurfaceHolder


    private lateinit var jpegCallback: Camera.PictureCallback

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var outputDirectory: File

    private lateinit var camera: androidx.camera.core.Camera
    private lateinit var cameraSelector: CameraSelector

    private var startTime = System.currentTimeMillis()
    private var clickCount = 0

    private val imageArrayList: MutableList<Bitmap?> = mutableListOf()
    private val customCameraAdapter by lazy { CustomImageAdapter(context, mutableListOf()) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }

    private var imageCapture: ImageCapture? = null

    constructor(
        context: Context
    ) : super(context) {

    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(attrs)
        initLayout()
    }

    fun setListener(listener: CustomCamera) {
        this.listener = listener
    }

    private fun init(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CCMultiple)
        try {
            mainLayoutId = R.layout.custom_camera
            showSnapButton = typedArray.getBoolean(R.styleable.CCMultiple_showSnapButton, false)
            snapButtonColor = typedArray.getColor(R.styleable.CCMultiple_snapButtonColor, context.resources.getColor(R.color.cardview_light_background))
            snapButtonSelectedColor = typedArray.getColor(R.styleable.CCMultiple_snapButtonSelectedColor, Color.parseColor("#CB0000"))
            showPreviewScreen = typedArray.getBoolean(R.styleable.CCMultiple_showPreviewScreen, false)
        } finally {
            typedArray.recycle()
        }
    }

    private fun initLayout() {
        if (isInEditMode) {
            return
        }

        val view = LayoutInflater.from(context).inflate(mainLayoutId, this)

        Log.d(TAG, "init ${this::listener.isInitialized}")
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProvider = cameraProviderFuture.get()

//        val cResolver: ContentResolver = context.contentResolver
//
//        if (!Settings.System.canWrite(context))
//            context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
//
//        Settings.System.putInt(cResolver, Settings.System.SCREEN_BRIGHTNESS, 1023)
//
//
//        val curBrightnessValue = Settings.System.getInt(
//            context.contentResolver,
//            Settings.System.SCREEN_BRIGHTNESS
//        )
//
//        Log.d(TAG, curBrightnessValue.toString())

        startCamera()


        imageRecyclerView.apply {
            adapter = customCameraAdapter
            layoutManager = linearLayoutManager
        }

        lifecycle.addObserver(this)

        captureImage.apply {
            isVisible = showSnapButton
            backgroundTintList = ColorStateList.valueOf(snapButtonColor)
        }

        //        outputDirectory = getOutputDirectory()
//        cameraExecutor = Executors.newSingleThreadExecutor()

        flashToggle.setOnClickListener {
//            if (camera.cameraInfo.torchState.value == TorchState.ON) {
//                Glide.with(context).load(R.drawable.ic_flash_off).into(flashToggle)
//                camera.cameraControl.enableTorch(false)
//            } else {
//                Glide.with(context).load(R.drawable.ic_flash_on).into(flashToggle)
////                camera.cameraControl.enableTorch(true)
//                imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
//            }
            when (imageCapture?.flashMode) {
                ImageCapture.FLASH_MODE_ON -> {
                    if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) {
                        Glide.with(context).load(R.drawable.ic_flash_off).into(flashToggle)
                        imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                    } else {
                        Glide.with(context).load(R.drawable.ic_flash_auto).into(flashToggle)
                        imageCapture?.flashMode = ImageCapture.FLASH_MODE_AUTO
                    }
                }
                ImageCapture.FLASH_MODE_OFF -> {
                    Glide.with(context).load(R.drawable.ic_flash_on).into(flashToggle)
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
                }
                ImageCapture.FLASH_MODE_AUTO -> {
                    Glide.with(context).load(R.drawable.ic_flash_off).into(flashToggle)
                    imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
                }
            }
        }

        rotateCamera.setOnClickListener {
            when (lensFacing) {
                CameraSelector.DEFAULT_BACK_CAMERA -> {
                    lensFacing = CameraSelector.DEFAULT_FRONT_CAMERA
                }
                CameraSelector.DEFAULT_FRONT_CAMERA -> lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
            }
            setDefaultFlashMode()
            startCamera()

        }

        confirmSelections.setOnClickListener {
            if (showPreviewScreen) {

            } else {
                listener.onConfirmImages(imageArrayList)
            }
        }

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
                    setButtonsClickable()
                    takePhoto()
                    return@setOnTouchListener true
                }
                else -> {return@setOnTouchListener true}
            }
        }

        surfaceView.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (clickCount == 2) {
                        clickCount = 1
                    } else {
                        clickCount++
                    }
                    if (clickCount == 1)
                        startTime = System.currentTimeMillis()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    val time = System.currentTimeMillis() - startTime
                    if (clickCount == 2 && time < 500) {
                        clickCount = 0
                        rotateCamera.callOnClick()
                    }
                    return@setOnTouchListener true
                }
                else -> {
                    return@setOnTouchListener true
                }
            }
        }

    }

    fun setBrightness(progress: Int): PorterDuffColorFilter? {
        return if (progress >= 100) {
            val value = (progress - 100) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 255, 255, 255), PorterDuff.Mode.SRC_OVER)
        } else {
            val value = (100 - progress) * 255 / 100
            PorterDuffColorFilter(Color.argb(value, 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
        }
    }

    private fun setDefaultFlashMode() {
        Glide.with(context).load(R.drawable.ic_flash_off).into(flashToggle)
        imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
    }

    private fun startCamera() {
        //   cameraProvider.unbindAll()
        cameraProviderFuture.addListener(Runnable {
            // Preview
            cameraSelector = lensFacing

            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(surfaceView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    private fun setButtonsClickable() {
        flashToggle.isClickable = !flashToggle.isClickable
        rotateCamera.isClickable = !rotateCamera.isClickable
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

        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA && imageCapture?.flashMode == ImageCapture.FLASH_MODE_ON) {
            frontFlash.makeVisible()
        }

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

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {

                        val rotatedBitmap = rotateBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        withContext(Dispatchers.Main) {
                            capturedImage.apply {
                                setImageBitmap(rotatedBitmap)
                            }
                            customCameraAdapter.addData(rotatedBitmap)
                            imageArrayList.add(rotatedBitmap)
                            frontFlash.makeGone()
                            val atTop = !imageRecyclerView.canScrollVertically(-1)

                            if (atTop) {
                                imageRecyclerView.scrollToPosition(0)
                            }
                            recaptureImage.isVisible = capturedImage.isVisible
                            confirmSelections.isVisible = true
                            imageCount.text = "${imageArrayList.size}"
                            setButtonsClickable()
                        }
                    }
                }

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

    override fun getLifecycle(): Lifecycle {
        val lifecycleRegistry = LifecycleRegistry(this);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        return lifecycleRegistry
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Log.d(TAG,event.toString())
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        imageArrayList.clear()
        cameraProvider.unbindAll()

    }

    private fun getOutputDirectory(): File {
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, "somename").apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }

    interface CustomCamera {
        fun onConfirmClicked()
        fun onConfirmImages(imageArrayList: MutableList<Bitmap?>)
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}