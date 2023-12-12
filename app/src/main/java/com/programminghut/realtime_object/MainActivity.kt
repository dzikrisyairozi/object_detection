package com.programminghut.realtime_object

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.content.Intent
import android.provider.MediaStore
import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.util.Log
import com.programminghut.realtime_object.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import android.graphics.drawable.BitmapDrawable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.hardware.camera2.CameraAccessException
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.OutputStream


class MainActivity : AppCompatActivity() {

    lateinit var labels:List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()
    lateinit var imageProcessor: ImageProcessor
    lateinit var bitmap:Bitmap
    lateinit var imageView: ImageView
    lateinit var handler: Handler
    lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    lateinit var model:SsdMobilenetV11Metadata1
    private lateinit var tflite: Interpreter
    private var isCameraOpen = false
    lateinit var startCameraButton: Button
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    private var defaultCameraCaptureSession: CameraCaptureSession? = null
    private var defaultCameraDevice: CameraDevice? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize labels, imageProcessor, model, handler, imageView, and textureView
        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = SsdMobilenetV11Metadata1.newInstance(this)

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        imageView = findViewById(R.id.imageView)
        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        // Initialize and set click listener for startCameraButton
        startCameraButton = findViewById(R.id.btnStartCamera)
        startCameraButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                open_camera()
            } else {
                get_permission()
            }
        }

        val selectImageButton: Button = findViewById(R.id.btnSelectImage)
        selectImageButton.setOnClickListener {
            pickImageFromGallery()
        }

        textureView = findViewById(R.id.textureView)
        textureView.surfaceTextureListener = textureViewListener
    }

    private val textureViewListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {

        }
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            val currentBitmap = textureView.bitmap
            if (currentBitmap != null) {
                // Log the dimensions of the captured bitmap
                Log.d("MainActivity", "Captured Bitmap: Width = ${currentBitmap.width}, Height = ${currentBitmap.height}")

                // Process the captured bitmap
                processCameraFeed(currentBitmap)
            } else {
                Log.e("MainActivity", "TextureView bitmap is null")
            }
        }


    }

    private fun processCameraFeed(bitmap: Bitmap) {
        Log.d("MainActivity", "processCameraFeed: Started processing camera feed")

        // Log the dimensions of the bitmap
        Log.d("MainActivity", "Bitmap dimensions: Width = ${bitmap.width}, Height = ${bitmap.height}")

        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)

        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

        // Log model outputs
        Log.d("MainActivity", "Number of detections: ${numberOfDetections.size}")
        for (i in numberOfDetections.indices) {
            Log.d("MainActivity", "Detection $i: Score = ${scores[i]}, Class = ${classes[i]}, Location = ${locations.slice(i * 4 until (i + 1) * 4)}")
        }

        var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        val h = mutable.height
        val w = mutable.width
        paint.textSize = h / 15f
        paint.strokeWidth = h / 85f
        var x: Int
        scores.forEachIndexed { index, score ->
            if (score > 0.5) {
                x = index * 4
                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.STROKE

                // Calculate the corrected drawing coordinates
                val left = locations[x + 1].coerceIn(0f, 1f) * w
                val top = locations[x].coerceIn(0f, 1f) * h
                val right = locations[x + 3].coerceIn(0f, 1f) * w
                val bottom = locations[x + 2].coerceIn(0f, 1f) * h

                // Draw the bounding box and label
                canvas.drawRect(left, top, right, bottom, paint)
                paint.style = Paint.Style.FILL
                canvas.drawText(labels[classes[index].toInt()] + " " + score, left, top, paint)

                // Log the drawing coordinates
                Log.d("MainActivity", "Drawing rect: left=$left, top=$top, right=$right, bottom=$bottom")
            }
        }


        runOnUiThread {
            imageView.setImageBitmap(mutable)
            Log.d("MainActivity", "Bitmap set on ImageView")
        }

        Log.d("MainActivity", "processCameraFeed: Finished processing camera feed")
    }



    override fun onDestroy() {
        super.onDestroy()
        close_camera()
        model.close()
    }

    fun close_camera() {
        try {
            if (isCameraOpen) {
                // Close the camera capture session
                cameraCaptureSession.close()
                // Set the session back to the default value
                cameraCaptureSession = defaultCameraCaptureSession ?: throw IllegalStateException("Default camera capture session is null")

                // Close the camera device
                cameraDevice.close()
                // Set the camera device back to the default value
                cameraDevice = defaultCameraDevice ?: throw IllegalStateException("Default camera device is null")

                // Update the button text and background color
                runOnUiThread {
                    startCameraButton.text = "Start Camera"
                    startCameraButton.setBackgroundColor(Color.TRANSPARENT) // Set the button background color to transparent
                    isCameraOpen = false
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun open_camera() {
        if (!isCameraOpen) {
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isNotEmpty()) {
                val cameraId = cameraIds[0] // Change this to the appropriate camera ID if needed
                try {
                    cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                        override fun onOpened(p0: CameraDevice) {
                            cameraDevice = p0

                            var surfaceTexture = textureView.surfaceTexture
                            var surface = Surface(surfaceTexture)

                            var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            captureRequest.addTarget(surface)

                            cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(p0: CameraCaptureSession) {
                                    cameraCaptureSession = p0 // Set the session
                                    // Update the default camera capture session
                                    defaultCameraCaptureSession = p0
                                    p0.setRepeatingRequest(captureRequest.build(), null, null)
                                }

                                override fun onConfigureFailed(p0: CameraCaptureSession) {
                                }
                            }, handler)
                            isCameraOpen = true
                            runOnUiThread {
                                startCameraButton.text = "Stop Camera"
                            }
                        }

                        override fun onDisconnected(p0: CameraDevice) {
                            cameraDevice.close()
                            // Set the camera device back to the default value
                            defaultCameraDevice = p0
                            isCameraOpen = false
                            runOnUiThread {
                                startCameraButton.text = "Start Camera"
                            }
                        }

                        override fun onError(p0: CameraDevice, p1: Int) {
                            cameraDevice.close()
                            // Set the camera device back to the default value
                            defaultCameraDevice = p0
                            isCameraOpen = false
                            runOnUiThread {
                                startCameraButton.text = "Start Camera"
                            }
                        }
                    }, handler)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            } else {
                Log.e("MainActivity", "No available camera found on the device.")
            }
        } else {
            // Camera is already open, so close it
            close_camera()
        }

        startCameraButton.text = "Stop Camera"
        startCameraButton.setTextColor(Color.WHITE)
        startCameraButton.setBackgroundColor(Color.RED) // Set the button background color to red
        isCameraOpen = true
    }



    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor: AssetFileDescriptor = assets.openFd("ssd_mobilenet_v1_1_metadata_1.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun initializeModel() {
        val tfliteOptions = Interpreter.Options()
        tflite = Interpreter(loadModelFile(), tfliteOptions)
    }



    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    companion object {
        private const val IMAGE_PICK_CODE = 999
        private const val MODEL_WIDTH = 300 // replace with your model's input width
        private const val MODEL_HEIGHT = 300 // replace with your model's input height
        private const val PIXEL_SIZE = 3 // for RGB image
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f
        private const val DETECTION_THRESHOLD = 0.5f
        private const val STORAGE_PERMISSION_CODE = 1001
        private const val PERMISSION_REQUEST_CODE = 101
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("MainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val imageUri = data?.data
            Log.d("MainActivity", "Image URI: $imageUri")
            imageUri?.let { uri ->
                processImage(uri)
            }
        }
    }


    private fun processImage(imageUri: Uri) {
        val imageStream = contentResolver.openInputStream(imageUri)
        val originalImage = BitmapFactory.decodeStream(imageStream)

        // Check if the image is loaded correctly
        if (originalImage != null) {
            Log.d("MainActivity", "Image loaded, size: ${originalImage.width} x ${originalImage.height}")

            // Resize the bitmap to the required input size for the model
            val processedImage = Bitmap.createScaledBitmap(originalImage, MODEL_WIDTH, MODEL_HEIGHT, true)

            // Process and display the image
            processAndDisplayImage(originalImage, processedImage)
        } else {
            Log.e("MainActivity", "Failed to decode selected image")
        }
    }



    private fun processAndDisplayImage(originalImage: Bitmap, processedImage: Bitmap) {
        Log.d("MainActivity", "Processing image")

        // Convert the resized Bitmap to TensorImage
        val tensorImage = TensorImage.fromBitmap(processedImage)

        // Process the image using the model
        val outputs = model.process(tensorImage)

        // Extract the output data
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray

        // Draw the results on the original image
        Log.d("MainActivity", "Model processing complete")
        drawDetectionResult(originalImage, locations, classes, scores, numberOfDetections)
    }



    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * MODEL_WIDTH * MODEL_HEIGHT * PIXEL_SIZE) // Float model: 4 bytes per pixel
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(MODEL_WIDTH * MODEL_HEIGHT)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until MODEL_WIDTH) {
            for (j in 0 until MODEL_HEIGHT) {
                val value = intValues[pixel++]

                // Normalize pixel value to [0,1] or [-1,1] as required by your model
                byteBuffer.putFloat(((value shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat(((value and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
            }
        }

        return byteBuffer
    }

    private fun drawDetectionResult(
        image: Bitmap,
        locations: FloatArray,
        classes: FloatArray,
        scores: FloatArray,
        numberOfDetections: FloatArray
    ) {
        Log.d("MainActivity", "Drawing detection results")
        val mutableImage = image.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableImage)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
        }
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40.0f
        }

        // Iterate over all detections
        for (i in 0 until numberOfDetections[0].toInt()) { // Make sure to iterate based on the number of detections
            val score = scores[i]
            if (score > DETECTION_THRESHOLD) {
                // The locations array contains the bounding box: [top, left, bottom, right]
                val top = locations[4 * i] * mutableImage.height
                val left = locations[4 * i + 1] * mutableImage.width
                val bottom = locations[4 * i + 2] * mutableImage.height
                val right = locations[4 * i + 3] * mutableImage.width

                // Draw the bounding box and label
                canvas.drawRect(left, top, right, bottom, paint)
                val label = labels[classes[i].toInt()]
                canvas.drawText("$label $score", left, top, textPaint)
            }
        }

        val drawable = BitmapDrawable(resources, mutableImage)
        runOnUiThread {
            // Hide the camera preview
            textureView.visibility = View.GONE

            // Show the processed image
            imageView.visibility = View.VISIBLE
            imageView.setImageDrawable(drawable)
            Log.d("MainActivity", "Drawable set on ImageView")

            findViewById<Button>(R.id.btnSaveImage).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    val savedImagePath = saveImageToGallery(mutableImage)
                    Toast.makeText(this@MainActivity, "Image saved to $savedImagePath", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveImageToGallery(bitmap: Bitmap): String {
        val filename = "detected_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var fileUri: Uri? = null
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        // Use the ContentResolver to save the image
        contentResolver.also { resolver ->
            fileUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = fileUri?.let { resolver.openOutputStream(it) }
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        return fileUri.toString() // Return the file path of the saved image
    }

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        }
    }

    fun get_permission() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    Toast.makeText(this, "Permission denied for: ${permissions[i]}", Toast.LENGTH_SHORT).show()
                }
            }

            if (allPermissionsGranted) {
                // All requested permissions are granted
                // You can proceed with opening the camera or saving images
            }
        }
    }

}