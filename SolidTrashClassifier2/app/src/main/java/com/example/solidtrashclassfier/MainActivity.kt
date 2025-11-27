package com.example.solidtrashclassfier

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.graphics.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import androidx.camera.view.PreviewView
import android.graphics.ImageFormat
import java.io.ByteArrayOutputStream
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var tflite: Interpreter
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var resultText: TextView

    // Adjust these based on your model!
    private val INPUT_SIZE = 96// Common: 96, 160, 224, 299, 320
    private val NUM_CLASSES = 3  // Adjust: plastic, paper, metal, glass, organic, other

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // This line should work now!

        resultText = findViewById(R.id.resultText)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Load model
        tflite = loadModel()

        // Start camera if permissions are granted
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun loadModel(): Interpreter {
        val assetFileDescriptor = assets.openFd("model.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        return Interpreter(buffer)
    }

//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder().build()
//            val imageAnalysis = ImageAnalysis.Builder()
//                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                .build()
//
//            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
//                val bitmap = imageProxy.toBitmap()
//                classifyImage(bitmap)
//                imageProxy.close()
//            })
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            try {
//                cameraProvider.unbindAll()
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview, imageAnalysis
//                )
//            } catch (exc: Exception) {
//                Log.e("Camera", "Use case binding failed", exc)
//            }
//        }, ContextCompat.getMainExecutor(this))
//    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Get reference to PreviewView
            val previewView: PreviewView = findViewById(R.id.cameraPreview)

            // Setup preview
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                val bitmap = imageProxy.toBitmap()
                classifyImage(bitmap)
                imageProxy.close()
            })

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                // Bind both preview and analysis
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

//    private fun classifyImage(bitmap: Bitmap) {
//        try {
//            // Preprocess image for model
//            val input = preprocessBitmap(bitmap)
//
//            // Output array
//            val output = Array(1) { FloatArray(NUM_CLASSES) }
//
//            // Run inference
//            tflite.run(input, output)
//
//            // Process results
//            val results = processOutput(output[0])
//
//            // Update UI on main thread
//            runOnUiThread {
//                resultText.text = "Trash: $results"
//            }
//
//        } catch (e: Exception) {
//            Log.e("Classification", "Error classifying image", e)
//        }
//    }

//    private fun classifyImage(bitmap: Bitmap) {
//        try {
//            // Preprocess for quantized model (int8)
//            val input = preprocessBitmapQuantized(bitmap)
//            val output = Array(1) { ByteArray(NUM_CLASSES) }
//
//            // Run inference
//            tflite.run(input, output)
//
//            // Process quantized output
//            val results = processOutputQuantized(output[0])
//
//            // Update UI
//            runOnUiThread {
//                resultText.text = "Trash: $results"
//            }
//
//        } catch (e: Exception) {
//            Log.e("Classification", "Error classifying image", e)
//        }
//    }
private fun interpretQuantizedOutput(output: ByteArray): String {
    var maxIndex = 0
    var maxValue = Byte.MIN_VALUE.toInt()

    for (i in output.indices) {
        val value = output[i].toInt()
        Log.d("ClassDebug", "Class $i: $value")
        if (value > maxValue) {
            maxValue = value
            maxIndex = i
        }
    }

    val probability = (maxValue - Byte.MIN_VALUE) / 255.0f

    // ⚠️ UPDATE THESE TO YOUR EXACT CLASS NAMES ⚠️
    val classes = arrayOf("glass", "metal","plastic") // Change these!

    return if (maxIndex < classes.size) {
        "${classes[maxIndex]} (${(probability * 100).toInt()}%)"
    } else {
        "Unknown (${(probability * 100).toInt()}%)"
    }
}

    private fun preprocessForQuantizedModel(bitmap: Bitmap): Array<Array<Array<ByteArray>>> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        Log.d("Preprocess", "Bitmap resized to: ${resizedBitmap.width}x${resizedBitmap.height}")

        val input = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { ByteArray(3) } } }

        for (x in 0 until INPUT_SIZE) {
            for (y in 0 until INPUT_SIZE) {
                val pixel = resizedBitmap.getPixel(x, y)
                input[0][x][y][0] = ((pixel shr 16 and 0xFF)).toByte() // R
                input[0][x][y][1] = ((pixel shr 8 and 0xFF)).toByte()  // G
                input[0][x][y][2] = ((pixel and 0xFF)).toByte()        // B
            }
        }
        return input
    }

    private fun classifyImage(bitmap: Bitmap) {
        try {
            Log.d("Classification", "Starting classification...")

            val input = preprocessForQuantizedModel(bitmap)
            val output = Array(1) { ByteArray(NUM_CLASSES) }

            // Run inference
            tflite.run(input, output)

            // Debug: Print raw output values
            Log.d("Classification", "Raw output: ${output[0].contentToString()}")

            val results = interpretQuantizedOutput(output[0])

            Log.d("Classification", "Final result: $results")

            runOnUiThread {
                resultText.text = "Trash: $results"
            }

        } catch (e: Exception) {
            Log.e("Classification", "Error classifying image", e)
        }
    }

    private fun preprocessBitmapQuantized(bitmap: Bitmap): Array<Array<Array<ByteArray>>> {
        // Resize to 96x96 (your model input size)
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        // Create input array for quantized model [1, 96, 96, 3] with INT8 values (0-255)
        val input = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { ByteArray(3) } } }

        for (x in 0 until INPUT_SIZE) {
            for (y in 0 until INPUT_SIZE) {
                val pixel = resizedBitmap.getPixel(x, y)
                // Direct RGB values (0-255) - NO normalization for quantized models
                input[0][x][y][0] = ((pixel shr 16 and 0xFF)).toByte() // R
                input[0][x][y][1] = ((pixel shr 8 and 0xFF)).toByte()  // G
                input[0][x][y][2] = ((pixel and 0xFF)).toByte()        // B
            }
        }
        return input
    }

//    private fun preprocessBitmap(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
//        // Resize bitmap to model input size
//        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
//
//        // Convert to float array (normalize to 0-1)
//        val input = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { FloatArray(3) } } }
//
//        for (x in 0 until INPUT_SIZE) {
//            for (y in 0 until INPUT_SIZE) {
//                val pixel = resizedBitmap.getPixel(x, y)
//                input[0][x][y][0] = ((pixel shr 16 and 0xFF) / 255.0f) // R
//                input[0][x][y][1] = ((pixel shr 8 and 0xFF) / 255.0f)  // G
//                input[0][x][y][2] = ((pixel and 0xFF) / 255.0f)        // B
//            }
//        }
//        return input
//    }
//private fun preprocessBitmap(bitmap: Bitmap): Array<ByteArray> {
//    // Resize bitmap to model input size
//    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
//
//    // Convert to byte array (INT8) instead of float array
//    val input = ByteArray(INPUT_SIZE * INPUT_SIZE * 3)
//    var index = 0
//
//    for (y in 0 until INPUT_SIZE) {
//        for (x in 0 until INPUT_SIZE) {
//            val pixel = resizedBitmap.getPixel(x, y)
//
//            // Convert to INT8 (0-255 range) - no normalization needed for quantized models
//            input[index++] = ((pixel shr 16 and 0xFF)).toByte() // R
//            input[index++] = ((pixel shr 8 and 0xFF)).toByte()  // G
//            input[index++] = ((pixel and 0xFF)).toByte()        // B
//        }
//    }
//
//    return arrayOf(input)
//}


    // Extension function to convert ImageProxy to Bitmap
//    private fun ImageProxy.toBitmap(): Bitmap {
//        val buffer = planes[0].buffer
//        buffer.rewind()
//        val bytes = ByteArray(buffer.capacity())
//        buffer.get(bytes)
//        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
//
//        // Rotate if needed
//        val matrix = Matrix()
//        matrix.postRotate(90f) // Adjust rotation based on camera orientation
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
//    }

    private fun processOutputQuantized(output: ByteArray): String {
        // Find class with highest score
        var maxIndex = 0
        var maxValue = Byte.MIN_VALUE.toInt()

        for (i in output.indices) {
            val value = output[i].toInt()
            if (value > maxValue) {
                maxValue = value
                maxIndex = i
            }
        }

        // Convert quantized output to probability (0-255 scale)
        val probability = (maxValue - Byte.MIN_VALUE) / 255.0f

        // Update these class names based on YOUR 3 trash types:
        val classes = arrayOf("Plastic", "Glass", "Metal") // CHANGE THESE!

        return if (maxIndex < classes.size) {
            "${classes[maxIndex]} (${(probability * 100).toInt()}%)"
        } else {
            "Unknown (${(probability * 100).toInt()}%)"
        }
    }

    // Extension function to convert ImageProxy to Bitmap - FIXED VERSION
    private fun ImageProxy.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val vuBuffer = planes[2].buffer // VU

        val ySize = yBuffer.remaining()
        val vuSize = vuBuffer.remaining()

        val nv21 = ByteArray(ySize + vuSize)

        yBuffer.get(nv21, 0, ySize)
        vuBuffer.get(nv21, ySize, vuSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, this.width, this.height), 100, out)
        val imageBytes = out.toByteArray()

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}