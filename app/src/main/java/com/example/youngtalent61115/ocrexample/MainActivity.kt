package com.example.youngtalent61115.ocrexample

import android.Manifest
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.Detector
import android.view.SurfaceHolder
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.hardware.Camera
import android.util.Log

import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import android.os.VibrationEffect
import android.os.Build
import android.content.Context.VIBRATOR_SERVICE
import android.support.v4.content.ContextCompat.getSystemService
import android.os.Vibrator
import android.view.View


class MainActivity : AppCompatActivity() {

    lateinit var mCameraSource : CameraSource

    private val RC_HANDLE_CAMERA_PERM = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            startCameraSource()
        } else {
            requestCameraPermission()
        }

    }

    private fun requestCameraPermission() {

        val permissions = arrayOf(Manifest.permission.CAMERA)

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM)
            return
        }

        val thisActivity = this
        View.OnClickListener {
            ActivityCompat.requestPermissions(thisActivity, permissions,
                    RC_HANDLE_CAMERA_PERM)
        }
    }


    private fun vibration() {
        val vibratorService = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibratorService.vibrate(500)
    }

    private fun startCameraSource() {
        //Create the TextRecognizer
        val textRecognizer = TextRecognizer.Builder(applicationContext).build()

        if (!textRecognizer.isOperational) {
            Log.w("beer", "Detector dependencies not loaded yet")
        } else {

            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setRequestedFps(10.0f)
                    .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                    .build()

            /**
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            camera_surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(applicationContext,
                                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(this@MainActivity,
                                    arrayOf(Manifest.permission.CAMERA),
                                    200)
                            return
                        }

                        mCameraSource.start(camera_surfaceView.holder)

                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

                }

                /**
                 * Release resources for cameraSource
                 */
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    mCameraSource.stop()
                }
            })

            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
                override fun release() {

                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 */
                override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                    val items = detections.detectedItems
                    if (items.size() != 0) {
                        text_view.post {
                            val stringBuilder = StringBuilder()
                            for (i in 0 until items.size()) {
                                val item = items.valueAt(i)
                                stringBuilder.append(item.value)
                                stringBuilder.append("\n")
                            }

                            val line = stringBuilder.toString()
                            val words = line.split("\\W+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            var replaceString: String
                            try {

                                var string = ""
                                if (words.size >= 2) {
                                    string = words[0] + words[1] + words[2]
                                    Log.d("beer","$string")
                                }
                                if (string.length == 12) {
                                    replaceString = string.replace('O', '0')//replaces all occurrences of a to e
                                    text_view.text = replaceString
                                    Log.d("beer","$replaceString")
                                    vibration()
                                }

                            } catch (e : Exception) {

                            }


                        }
                    }
                }
            })

        }
    }
}
