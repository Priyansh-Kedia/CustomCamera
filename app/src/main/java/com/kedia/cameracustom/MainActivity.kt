package com.kedia.cameracustom

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.kedia.customcamera.CCMultiple

class MainActivity : AppCompatActivity(), CCMultiple.CustomCamera {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val customCamera = CCMultiple(this, this)
    }

    override fun onConfirmClicked() {

    }

    override fun onConfirmImages(imageArrayList: MutableList<Bitmap?>) {
        Log.d("TAG!!!!", "$imageArrayList ${imageArrayList.size}")
    }


}
