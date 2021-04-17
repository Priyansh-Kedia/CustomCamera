package com.kedia.cameracustom

import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.kedia.customcamera.CCMultiple
import com.kedia.customcamera.utils.CustomMultiple
import kotlinx.android.synthetic.main.activity_main2.*

class Main2Activity : AppCompatActivity(), CustomMultiple {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        cc.setListener(this)
    }

    override fun onConfirmImagesClicked(imageArrayList: MutableList<Uri>) {
        Log.d("TAG", "onConfirmImagesClicked: ")
    }

    override fun onCameraFrameReceived(bitmap: Bitmap) {
        Log.d("TAG", "onCameraFrameReceived: ")
    }

}