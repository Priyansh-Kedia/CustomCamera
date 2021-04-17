
# CustomCamera Library
[![](https://jitpack.io/v/Priyansh-Kedia/CustomCamera.svg)](https://jitpack.io/#Priyansh-Kedia/CustomCamera)



CCMultiple library provides you with the convenience of accessing the camera of your android device using the latest CameraX API, with just a few lines of code. 


Add it in your root build.gradle at the end of repositories:

		allprojects {
			repositories {
			...
				maven { url 'https://jitpack.io' }
			}
		}


Add the dependency

	dependencies {
		   implementation 'com.github.Priyansh-Kedia:CustomCamera:<latest_version>'
		}
		


You can include the camera directly in the XML code, by 

    <androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"  
	 xmlns:app="http://schemas.android.com/apk/res-auto"  
	  xmlns:tools="http://schemas.android.com/tools"  
	  android:layout_width="match_parent"  
	  android:layout_height="match_parent">  
  
  
		 <com.kedia.customcamera.CCMultiple  android:id="@+id/cc"  
		  android:layout_width="match_parent"  
		  android:layout_height="match_parent"  
		  app:showFlashButton="true"  
		  app:showNoPermissionToast="true"  
		  app:streamContinuously="true"  
		  app:showRotateCamera="true" />  
  
	</androidx.constraintlayout.widget.ConstraintLayout>
	

This library provides with customisable features, which provides with a better control over the features.

These features include:

 1. Snap button visibility:- You can set the snap button visibility, just by setting the value of `showSnapButton` to `true` in your XML code. You can also set the value of this attribute by using `setSnapButtonVisibility` from your Java / Kotlin code.
 2. Snap button color:- You can set the snap button color, just by setting the value of `snapButtonColor` to a color resource in your XML code. You can also set the value of this attribute by using `setSnapButtonColor` from your Java / Kotlin code. 
 3. Snap button selected color:- This lets you set the color of the snap button when it is clicked. You can set the value by `snapButtonSelectedColor` to a color resource in your XML code. You can also set the value of this attribute by using `setSnapButtonSelectedColor` from your Java / Kotlin code. 
 4. Permission toast visibility:- You can set if the user should see the toast message if the camera permission is not granted. You can set the value of this attribute by setting `showNoPermissionToast` to `true` or `false`
 5. Rotate camera button visibility:- You can control the visibility of the rotate camera button. This can be done by setting `showRotateCamera` to `true` from the XML code, or by setting `setRotateVisibility` from your Java / Kotlin code.
 6. Flash button visibility:- You can control the visibility of the flash button. This can be done by setting `showFlashButton` to `true` from the XML code, or by setting `showFlashToggle` from your Java / Kotlin code. 
 7. Captured images number:- You can control if the user can capture single or multiple photos using the camera. This can be done by setting `captureSingle` or `captureMultiple` to true. **Remember**, if you set the value of both these attributes to `true` at the same time, it will lead to a runtime exception. 
 8. Image deselection option:- You can give the user the freedom to delete the captured images in case of multiple captures. This can be done by setting `showImageDeselectionOption` to `true` in your XML code. If the value of this attribute is set to `true`, the user will be able to see a small cross button at the top-right corner of the captured images, which can be used to delete a particular click. The default value of this attribute is `true`.

# Set a listener to receive callbacks
To receive the callbacks from the camera, you need to implement the interface `CustomMultiple`. You can do this by using `setListener` from your Java / Kotlin code. 

The callback which can be invoked, is `onConfirmImagesClicked`, which gets a list of `Uri`, you can extract the images using the given `Uri`.

*The size of the list is 1 in case of single image capture.*

## Receive continuous frames from the camera

To receive continuous frames from the camera, set the value of `streamContinuously` to `true` from the XML code. The value of this attribute can also be set using `setContinuousStreaming` from Java / Kotlin code. 
*Remember that flash button cannot be visible when receiving continuous frames from the camera.*

Currently the library supports only `STRATEGY_KEEP_ONLY_LATEST` of CameraX, which keeps only the latest image captured, until the image is closed. You can close the image by using the `closeImage()` method from Java / Kotlin code. Only after closing the image, will the next frame be available. 

The continuous frames of the camera can be received in the callback `onCameraFrameReceived` which has the current bitmap.



### Found this library useful? :heart:

Support it by joining [stargazers](https://github.com/Priyansh-Kedia/CustomCamera/stargazers) for this repository. :star2:
