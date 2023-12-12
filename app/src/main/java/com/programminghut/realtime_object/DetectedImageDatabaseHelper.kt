package com.programminghut.realtime_object

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor

class DetectedImageDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "DetectedImages.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "detected_images"
        private const val COLUMN_ID = "id"
        private const val COLUMN_IMAGE_PATH = "image_path"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableStatement = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_IMAGE_PATH TEXT,
                $COLUMN_TIMESTAMP TEXT
            )
        """.trimIndent()
        db.execSQL(createTableStatement)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database version upgrades here
    }

    fun addDetectedImage(imagePath: String, timestamp: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_IMAGE_PATH, imagePath)
            put(COLUMN_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_NAME, null, contentValues)
        db.close()
    }

    fun getAllDetectedImages(): List<DetectedImage> {
        val detectedImages = mutableListOf<DetectedImage>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            val indexPath = cursor.getColumnIndex(COLUMN_IMAGE_PATH)
            val indexTimestamp = cursor.getColumnIndex(COLUMN_TIMESTAMP)

            while (!cursor.isAfterLast) {
                if (indexPath != -1 && indexTimestamp != -1) {
                    val imagePath = cursor.getString(indexPath)
                    val timestamp = cursor.getString(indexTimestamp)
                    detectedImages.add(DetectedImage(imagePath, timestamp))
                }
                cursor.moveToNext()
            }
        }
        cursor.close()
        db.close()
        return detectedImages
    }

}
