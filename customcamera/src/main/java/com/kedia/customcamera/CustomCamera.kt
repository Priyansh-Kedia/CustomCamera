package com.kedia.customcamera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.camera2.CameraManager
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.LayoutRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.Rotate
import kotlinx.android.synthetic.main.custom_camera.view.*
import androidx.recyclerview.widget.LinearLayoutManager


class CustomCamera : FrameLayout, SurfaceHolder.Callback {

    @LayoutRes
    private var mainLayoutId = 0

    private lateinit var surfaceHolder: SurfaceHolder


    private lateinit var jpegCallback: Camera.PictureCallback
    private val TAG = "TAG!!!!"

    private lateinit var camera: Camera

    private val imageArrayList: MutableList<Bitmap?> = mutableListOf()
    private val customCameraAdapter by lazy { CustomImageAdapter(context, mutableListOf()) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false) }


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

        surfaceHolder = surfaceView.holder

        imageRecyclerView.apply {
            adapter = this@CustomCamera.customCameraAdapter
            layoutManager = linearLayoutManager
        }

        surfaceHolder.addCallback(this)

        jpegCallback = Camera.PictureCallback { bytes, camera ->

            val decodeBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val now = System.currentTimeMillis()
            val rotatedBitmap = rotateBitmap(decodeBitmap)

          //  Glide.with(context).load(decodeBitmap).transform(Rotate(90)).into(capturedImage)
            capturedImage.setImageBitmap(rotatedBitmap)
            imageArrayList.add(rotatedBitmap)
            customCameraAdapter.addData(rotatedBitmap)

            Log.d("TAG!!!!",(imageArrayList).toString())
            capturedImage.visibility = View.VISIBLE
            surfaceView.visibility = View.GONE
            captureImage.visibility = View.GONE
            recaptureImage.visibility = View.VISIBLE
        }

        recaptureImage.setOnClickListener {
            initialiseCamera()
            capturedImage.visibility = View.GONE
            surfaceView.visibility = View.VISIBLE
            captureImage.visibility = View.VISIBLE
            recaptureImage.visibility = View.GONE
        }

        captureImage.setOnClickListener {
            captureImage()
        }

    }

    private fun rotateBitmap(decodeBitmap: Bitmap?): Bitmap? {
        val width = decodeBitmap?.width
        val height = decodeBitmap?.height

        val matrix = Matrix()
        matrix.postRotate(90f)
        val rotatedImage = Bitmap.createBitmap(decodeBitmap!!, 0, 0, width!!, height!!, matrix, true)
        decodeBitmap.recycle()
        return rotatedImage
    }

    private fun captureImage() {
        camera.takePicture(null, null, jpegCallback)
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        initialiseCamera()
    }

    private fun initialiseCamera() {
        camera = Camera.open()

        val parameters = camera.parameters
        camera.setDisplayOrientation(90)
        parameters.previewFrameRate = 30
        parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE

        camera.setPreviewDisplay(surfaceHolder)
        camera.parameters = parameters
        camera.enableShutterSound(false)
        camera.startPreview()
    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {

    }

    override fun surfaceCreated(p0: SurfaceHolder) {

    }

}