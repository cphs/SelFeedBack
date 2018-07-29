package com.cphs.selfeedback

import android.app.Activity
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.cphs.selfeedback.databinding.ActivityMainBinding
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark


class MainActivity : AppCompatActivity() {

    private var mBinding: ActivityMainBinding? = null

    companion object {
        const val REQUEST_IMAGE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mBinding?.btCamera?.setOnClickListener({
            openCamera()
        })
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK) {
            val extras = data.extras
            extras?.let {
                processImage(it["data"] as Bitmap)
            }
        }
    }

    private fun processImage(image: Bitmap) {
        mBinding?.ivImage?.setImageBitmap(image)

        val firebaseVisionImage = FirebaseVisionImage.fromBitmap(image)


        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.15f)
                .setTrackingEnabled(true)
                .build()

        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)

        detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener {
                    processFace(it)
                }
                .addOnFailureListener {
                    Log.e("error", it.message)
                }

    }

    private fun processFace(faces: List<FirebaseVisionFace>) {
        for (face in faces) {
            val bounds = face.boundingBox
            val rotY = face.headEulerAngleY  // Head is rotated to the right rotY degrees
            val rotZ = face.headEulerAngleZ  // Head is tilted sideways rotZ degrees

            // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
            // nose available):
            val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
            leftEar?.let {
                val leftEarPos = it.position
            }

            // If classification was enabled:
            val smileProbe = if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                face.smilingProbability
            } else 0F

            val rightEyeOpenProb = if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                face.rightEyeOpenProbability
            } else 0F

            val leftEyeOpenProb = if (face.leftEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                face.rightEyeOpenProbability
            } else 0F

            // If face tracking was enabled:
            if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                val id = face.trackingId
            }
            setView(smileProbe > 0.4, rightEyeOpenProb > 0.3, leftEyeOpenProb > 0.3)
        }
    }

    private fun setView(smile: Boolean, rightEyeOpened: Boolean, leftEyeOpened: Boolean) {
        var result = "You look "
        if (smile) {
            result += "great "
            if (rightEyeOpened && leftEyeOpened) {
                result += "with awesome eyes "
            } else if (rightEyeOpened || leftEyeOpened) {
                result += "when winkling"
            }
        } else if (rightEyeOpened && leftEyeOpened) {
            result += "scary "
        } else {
            result += "sad "
        }
        mBinding?.tvResult?.text = result
        mBinding?.tvResult?.visibility = View.VISIBLE
        mBinding?.ivImage?.visibility = View.VISIBLE
        mBinding?.tvWelcome?.visibility = View.GONE
    }
}
