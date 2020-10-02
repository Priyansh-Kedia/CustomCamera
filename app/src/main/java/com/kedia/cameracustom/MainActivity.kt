package com.kedia.cameracustom

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kedia.customcamera.CCMultiple
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), CCMultiple.CustomCamera {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cc.setListener(this)
    }

    override fun onConfirmImages(imageArrayList: MutableList<Bitmap?>) {
        TODO("Not yet implemented")
    }

    override fun onGalleryClicked() {
        TODO("Not yet implemented")
    }
}
