package com.kedia.customcamera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.DimenRes
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.common.util.concurrent.ListenableFuture
import com.kedia.customcamera.utils.*
import kotlinx.android.synthetic.main.custom_camera.view.*
import kotlinx.android.synthetic.main.gallery_bottom_sheet.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.lang.Math.abs


class CCMultiple : FrameLayout, CustomImageAdapter.CustomAdapterClick, LifecycleOwner {



    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var listener: CustomMultiple
    private lateinit var preview: Preview
//    private lateinit var jpegCallback: Camera.PictureCallback
//    private lateinit var cameraExecutor: ExecutorService
//    private lateinit var outputDirectory: File
    private lateinit var camera: androidx.camera.core.Camera
    private var progressDialogFragment: ProgressDialogFragment? = null
    private lateinit var cameraSelector: CameraSelector

    private val imageArrayList: MutableList<Bitmap?> = mutableListOf()
    private val uriArrayList: MutableList<Uri> = mutableListOf()
    private val customCameraAdapter by lazy { CustomImageAdapter(context, mutableListOf(), this) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }
    private val GALLERY_IMAGE_PICKER = 1

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    @LayoutRes
    private var mainLayoutId = 0
    private var startTime = System.currentTimeMillis()
    private var clickCount = 0
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
    private var isPermissionGranted = ActivityCompat.checkSelfPermission(context, REQUIRED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED
    private var numberOfTimesChecked = 0

    @DimenRes
    private var peekHeight = R.dimen.imagePickerPeekHeight

    private var snapButtonColor: Int = context.resources.getColor(R.color.cardview_light_background)
    private var snapButtonSelectedColor: Int = context.resources.getColor(R.color.cardview_light_background)
    private var showSnapButton: Boolean = false
    private var showPreviewScreen: Boolean = false
    private var imageCapture: ImageCapture? = null
    private var showNoPermissionToast = false
    private var showImageDeselectionOption = false
    private var showRotateCamera = false


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

    fun setListener(listener: CustomMultiple) {
        this.listener = listener
    }

    private fun init(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CCMultiple)
        try {
            mainLayoutId = R.layout.custom_camera
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            showSnapButton = typedArray.getBoolean(R.styleable.CCMultiple_showSnapButton, false)
            snapButtonColor = typedArray.getColor(R.styleable.CCMultiple_snapButtonColor, context.resources.getColor(R.color.cardview_light_background))
            snapButtonSelectedColor = typedArray.getColor(R.styleable.CCMultiple_snapButtonSelectedColor, Color.parseColor("#CB0000"))
            showPreviewScreen = typedArray.getBoolean(R.styleable.CCMultiple_showPreviewScreen, false)
            showNoPermissionToast = typedArray.getBoolean(R.styleable.CCMultiple_showNoPermissionToast, false)
            showImageDeselectionOption = typedArray.getBoolean(R.styleable.CCMultiple_showImageDeselectionOption, false)
            showRotateCamera = typedArray.getBoolean(R.styleable.CCMultiple_showRotateCamera, false)
        } finally {
            typedArray.recycle()
        }
    }

    private fun initLayout() {
        if (isInEditMode) {
            return
        }

        val view = LayoutInflater.from(context).inflate(mainLayoutId, this)

        checkRequirements()
    }

    private fun checkRequirements() {
        if (isPermissionGranted)
            init()
        else {
            numberOfTimesChecked += 1
            requirePermission()
        }
    }

    private fun requirePermission() {
        ActivityCompat.requestPermissions(context as Activity, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        isPermissionGranted = ActivityCompat.checkSelfPermission(context, REQUIRED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED
        if (numberOfTimesChecked < 3 && !isPermissionGranted)
            checkRequirements()
        else {
            if (numberOfTimesChecked >= 3 && !isPermissionGranted && hasWindowFocus) {
                if (showNoPermissionToast) {
                    Toast.makeText(context,"You need to grant permission to access camera",Toast.LENGTH_SHORT).show()
                    logE("Camera permission declined")
                }
                else
                    logE("Camera permission declined")
            }
            else {
                if (isPermissionGranted)
                    init()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(context, REQUIRED_PERMISSIONS[0]) == PackageManager.PERMISSION_GRANTED
    }

    private fun setCamera() {
            cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()

            startCamera()
    }

    private fun init() {
        setCamera()
        initViews()

        setListeners()
    }

    private fun initViews() {
        imageRecyclerView.apply {
            adapter = customCameraAdapter
            layoutManager = linearLayoutManager
        }


        captureImage.apply {
            isVisible = showSnapButton
            backgroundTintList = ColorStateList.valueOf(snapButtonColor)
        }

        rotateCamera.isVisible = showRotateCamera

        bottomSheetBehavior.peekHeight = resources.getDimensionPixelOffset(peekHeight)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
    }

    private val bottomSheetCallback by lazy {
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {

                }
            }

        }
    }

    private fun setListeners() {
        flashToggle.setOnClickListener {
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
                if (::listener.isInitialized)
                    listener.onConfirmImagesClicked(uriArrayList)
                else
                    logE("The listener has not been initialised")
            }
        }

        gallery.setOnClickListener {
            if (::listener.isInitialized)
                listener.onGalleryClicked()
            else
                logE("The listener has not been initialised")
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

        surfaceView.setOnTouchListener { _, motionEvent ->
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

    private fun changeBrightness(type: BRIGHTNESS) {
        if (context is ContextWrapper) {
            val attrs = (context as Activity).window.attributes
            attrs.screenBrightness = if (type == BRIGHTNESS.HIGH) 1f else -1f
            (context as Activity).window.attributes = attrs
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
                logE("Use case binding failed $exc")
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

        logV("Capturing image")

        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA && imageCapture.flashMode == ImageCapture.FLASH_MODE_ON) {
            frontFlash.makeVisible()
            changeBrightness(BRIGHTNESS.HIGH)
        }

        imageCapture.takePicture(ContextCompat.getMainExecutor(context),object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                // Use the image, then make sure to close it.
                val buffer = image.planes[0].buffer
                val cap = buffer.capacity()
                val bytes = ByteArray(buffer.capacity()).also { buffer.get(it) }
                logV("Bytes taken")
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {

                        val rotatedBitmap = rotateBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                        val uri = rotatedBitmap?.let { getUri(context, it) }
                        uri?.let { uriArrayList.add(it) }
                        withContext(Dispatchers.Main) {
                            capturedImage.apply {
                                setImageBitmap(rotatedBitmap)
                            }

                            changeBrightness(BRIGHTNESS.LOW)
                            customCameraAdapter.addData(rotatedBitmap)
                            frontFlash.makeGone()
                            val atTop = !imageRecyclerView.canScrollVertically(-1)

                            if (atTop) {
                                imageRecyclerView.scrollToPosition(0)
                            }
                            recaptureImage.isVisible = capturedImage.isVisible
                            confirmSelections.isVisible = true
                            imageCount.text = "${uriArrayList.size}"
                            setButtonsClickable()
                        }
                    }
                }

                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                val errorType = exception.imageCaptureError
                logV(exception.localizedMessage)
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun getRotationAngle(decodeByteArray: InputStream) {
        val exifInterface = ExifInterface(decodeByteArray)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                // Todo() Add rotate method with 90 angle
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                // Todo() Add rotate method with 180 angle
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                // Todo() Add rotate method with 270 angle
            }
            ExifInterface.ORIENTATION_NORMAL -> {
                // Todo() Add rotate method with 0 angle
            }
        }
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

    fun getPath(): Uri? {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    override fun getLifecycle(): Lifecycle {
        val lifecycleRegistry = LifecycleRegistry(this);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        return lifecycleRegistry
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        imageArrayList.clear()
        cameraProvider.unbindAll()

    }

    override fun onDeleteImageClicked(adapterPosition: Int, bitmap: Bitmap?) {
        customCameraAdapter.removeItem(adapterPosition)
        imageArrayList.remove(bitmap)
        imageCount.text = "${customCameraAdapter.itemCount}"
        if (customCameraAdapter.itemCount == 0)
            confirmSelections.makeGone()
    }

    private fun getOutputDirectory(): File {
        val mediaDir = (context as ContextWrapper).externalMediaDirs.firstOrNull()?.let {
            File(it, "IMG_").apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else context.filesDir
    }

    interface CustomMultiple {
        fun onConfirmImagesClicked(imageArrayList: MutableList<Uri>)
        fun onGalleryClicked()
    }

    companion object {
        private const val TAG = "CustomCamera"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    enum class BRIGHTNESS {
        HIGH,
        LOW
    }
}