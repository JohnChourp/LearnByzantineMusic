package com.johnchourp.learnbyzantinemusic.scan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.johnchourp.learnbyzantinemusic.BaseActivity
import com.johnchourp.learnbyzantinemusic.R
import java.io.File

class InAppCameraActivity : BaseActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var cancelButton: Button
    private lateinit var statusText: TextView

    private var imageCapture: ImageCapture? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_in_app_camera)

        previewView = findViewById(R.id.camera_preview_view)
        captureButton = findViewById(R.id.camera_capture_btn)
        cancelButton = findViewById(R.id.camera_cancel_btn)
        statusText = findViewById(R.id.camera_status_text)

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        captureButton.setOnClickListener {
            capturePhoto()
        }

        bindCameraUseCases()
    }

    private fun bindCameraUseCases() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val cameraProvider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setJpegQuality(92)
                .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
            statusText.text = getString(R.string.byz_scan_camera_status_ready)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun capturePhoto() {
        val capture = imageCapture ?: return
        val outputFile = createTempImageFile() ?: run {
            statusText.text = getString(R.string.byz_scan_capture_failed)
            return
        }

        captureButton.isEnabled = false
        statusText.text = getString(R.string.byz_scan_camera_status_capturing)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = FileProvider.getUriForFile(
                        this@InAppCameraActivity,
                        "$packageName.fileprovider",
                        outputFile
                    )
                    val result = Intent().putExtra(EXTRA_CAPTURED_URI, uri.toString())
                    setResult(RESULT_OK, result)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    captureButton.isEnabled = true
                    statusText.text = getString(R.string.byz_scan_capture_failed)
                    Toast.makeText(
                        this@InAppCameraActivity,
                        getString(R.string.byz_scan_capture_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun createTempImageFile(): File? {
        return runCatching {
            val directory = File(cacheDir, "byz_scan_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            File.createTempFile("byz_scan_", ".jpg", directory)
        }.getOrNull()
    }

    companion object {
        const val EXTRA_CAPTURED_URI = "captured_uri"

        fun extractUri(resultData: Intent?): Uri? {
            val uriString = resultData?.getStringExtra(EXTRA_CAPTURED_URI) ?: return null
            return runCatching { Uri.parse(uriString) }.getOrNull()
        }
    }
}
