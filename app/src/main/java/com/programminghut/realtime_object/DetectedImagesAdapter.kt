package com.programminghut.realtime_object

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.programminghut.realtime_object.R

class DetectedImagesAdapter(private var detectedImages: List<DetectedImage> = listOf(), private val deleteCallback: ImageDeleteCallback) : RecyclerView.Adapter<DetectedImagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewDetected)
    }

    interface ImageDeleteCallback {
        fun onImageDeleted()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.detected_image_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val detectedImage = detectedImages[position]
        Log.d("DetectedImagesAdapter", "Loading image from path: ${detectedImage.imagePath}")

        Glide.with(holder.imageView.context)
            .load(detectedImage.imagePath)
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            val dialog = Dialog(holder.imageView.context)
            dialog.setContentView(R.layout.dialog_image_showcase)

            val imageViewShowcase: ImageView = dialog.findViewById(R.id.imageViewShowcase)
            val btnDeleteImage: ImageButton = dialog.findViewById(R.id.btnDeleteImage)

            Glide.with(holder.imageView.context)
                .load(detectedImage.imagePath)
                .error(R.drawable.ic_error_placeholder)
                .into(imageViewShowcase)

            btnDeleteImage.setOnClickListener {
                // Call the method to delete the image
                deleteImage(detectedImage, holder.imageView.context)
                dialog.dismiss() // Dismiss the dialog after deletion
            }

            dialog.show()
        }

    }

    override fun getItemCount() = detectedImages.size

    fun updateData(newDetectedImages: List<DetectedImage>) {
        detectedImages = newDetectedImages
        notifyDataSetChanged()
    }


    fun deleteImage(detectedImage: DetectedImage, context: Context) {
        val databaseHelper = DetectedImageDatabaseHelper(context)
        databaseHelper.deleteDetectedImage(detectedImage.imagePath)

        // Display a toast message
        Toast.makeText(context, "Image history is successfully deleted", Toast.LENGTH_SHORT).show()

        // Notify the activity to refresh the RecyclerView
        deleteCallback.onImageDeleted()
    }

}
