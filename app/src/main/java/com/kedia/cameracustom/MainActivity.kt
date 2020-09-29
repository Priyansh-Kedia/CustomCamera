package com.kedia.cameracustom

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.kedia.customcamera.CCMultiple
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), CCMultiple.CustomCamera {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cc.setListener(this)
    }

    override fun onConfirmImages(imageArrayList: MutableList<Bitmap?>) {
        Log.d("TAG!!!!", "$imageArrayList ${imageArrayList.size}")
    }

    override fun onGalleryClicked() {
        Log.d("TAG!!!!","clicked ")
    }


}
