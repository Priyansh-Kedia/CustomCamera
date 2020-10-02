package com.kedia.cameracustom

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kedia.customcamera.CCMultiple

class Main2Activity : AppCompatActivity(), CCMultiple.CustomMultiple {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
    }

    override fun onConfirmImagesClicked(imageArrayList: MutableList<Bitmap?>) {
        TODO("Not yet implemented")
    }

    override fun onGalleryClicked() {
        TODO("Not yet implemented")
    }
}