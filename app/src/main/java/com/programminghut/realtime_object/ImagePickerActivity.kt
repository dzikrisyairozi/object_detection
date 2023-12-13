package com.programminghut.realtime_object

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.realtime_object.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Date
import java.util.Locale

class ImagePickerActivity : AppCompatActivity() {

    private val IMAGE_PICK_CODE = 999
    private lateinit var imageView: ImageView
    private lateinit var model: SsdMobilenetV11Metadata1
    lateinit var labels:List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)

        imageView = findViewById(R.id.selectedImageView)
        labels = FileUtil.loadLabels(this, "labels.txt")

        val btnOpenGallery: Button = findViewById(R.id.btnOpenGallery)
        btnOpenGallery.setOnClickListener {
            pickImageFromGallery()
        }

        // Initialize your model here
        model = SsdMobilenetV11Metadata1.newInstance(this)
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            val imageUri = data?.data
            imageUri?.let {
                processImage(it)
            }
        }
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

    private fun processImage(imageUri: Uri) {
        val imageStream = contentResolver.openInputStream(imageUri)
        val originalImage = BitmapFactory.decodeStream(imageStream)

        // Check if the image is loaded correctly
        if (originalImage != null) {
            Log.d("MainActivity", "Image loaded, size: ${originalImage.width} x ${originalImage.height}")

            // Resize the bitmap to the required input size for the model
            val processedImage = Bitmap.createScaledBitmap(originalImage,
                ImagePickerActivity.MODEL_WIDTH,
                ImagePickerActivity.MODEL_HEIGHT, true)

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
            if (score > ImagePickerActivity.DETECTION_THRESHOLD) {
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

            // Show the processed image
            imageView.visibility = View.VISIBLE
            imageView.setImageDrawable(drawable)
            Log.d("MainActivity", "Drawable set on ImageView")

            findViewById<Button>(R.id.btnSaveImage).apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    val savedImagePath = saveImageToGallery(mutableImage)
                    Toast.makeText(this@ImagePickerActivity, "Image saved to $savedImagePath", Toast.LENGTH_SHORT).show()
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

        // Save the image details in the database (consider doing this in a background thread)
        fileUri?.let {
            val savedImagePath = it.toString()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            Thread {
                val databaseHelper = DetectedImageDatabaseHelper(this)
                databaseHelper.addDetectedImage(savedImagePath, timestamp)
            }.start()
            return savedImagePath // Return the file path of the saved image
        }

        return "" // Return an empty string if saving the image failed
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * ImagePickerActivity.MODEL_WIDTH *ImagePickerActivity.MODEL_HEIGHT * ImagePickerActivity.PIXEL_SIZE) // Float model: 4 bytes per pixel
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(ImagePickerActivity.MODEL_WIDTH * ImagePickerActivity.MODEL_HEIGHT)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until ImagePickerActivity.MODEL_WIDTH) {
            for (j in 0 until ImagePickerActivity.MODEL_HEIGHT) {
                val value = intValues[pixel++]

                // Normalize pixel value to [0,1] or [-1,1] as required by your model
                byteBuffer.putFloat(((value shr 16 and 0xFF) - ImagePickerActivity.IMAGE_MEAN) / ImagePickerActivity.IMAGE_STD)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - ImagePickerActivity.IMAGE_MEAN) / ImagePickerActivity.IMAGE_STD)
                byteBuffer.putFloat(((value and 0xFF) - ImagePickerActivity.IMAGE_MEAN) / ImagePickerActivity.IMAGE_STD)
            }
        }

        return byteBuffer
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close() // Remember to close your model
    }
}