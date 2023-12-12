package com.programminghut.realtime_object

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DetectedHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DetectedImagesAdapter
    private lateinit var databaseHelper: DetectedImageDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detected_history_activity)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DetectedImagesAdapter()
        recyclerView.adapter = adapter

        databaseHelper = DetectedImageDatabaseHelper(this)
        loadDetectedImages()
    }

    private fun loadDetectedImages() {
        val detectedImages = databaseHelper.getAllDetectedImages()
        adapter.updateData(detectedImages)
    }
}
