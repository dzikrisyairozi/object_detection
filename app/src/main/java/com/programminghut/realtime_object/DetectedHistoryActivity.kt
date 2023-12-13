package com.programminghut.realtime_object

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

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

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_history
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_history -> {
                    val intent = Intent(this, DetectedHistoryActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_camera -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_image_picker -> {
                    val intent = Intent(this, ImagePickerActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        databaseHelper = DetectedImageDatabaseHelper(this)
        loadDetectedImages()
    }

    private fun loadDetectedImages() {
        val detectedImages = databaseHelper.getAllDetectedImages()
        adapter.updateData(detectedImages)
    }
}
