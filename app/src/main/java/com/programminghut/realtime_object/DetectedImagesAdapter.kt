package com.programminghut.realtime_object

import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.programminghut.realtime_object.R

class DetectedImagesAdapter(private var detectedImages: List<DetectedImage> = listOf()) : RecyclerView.Adapter<DetectedImagesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewDetected)
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

            // Set the layout parameters for the dialog window
            dialog.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            val imageViewShowcase: ImageView = dialog.findViewById(R.id.imageViewShowcase)
            Glide.with(holder.imageView.context)
                .load(detectedImage.imagePath) // This is the content URI
                .error(R.drawable.ic_error_placeholder) // Use an actual error drawable
                .into(imageViewShowcase)


            dialog.show()
        }

    }

    override fun getItemCount() = detectedImages.size

    fun updateData(newDetectedImages: List<DetectedImage>) {
        detectedImages = newDetectedImages
        notifyDataSetChanged()
    }
}
