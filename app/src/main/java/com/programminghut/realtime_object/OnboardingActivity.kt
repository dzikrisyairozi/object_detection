package com.programminghut.realtime_object

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorLayout: LinearLayout
    private lateinit var getStartedButton: Button
    private lateinit var dots: Array<ImageView>
    private val numPages = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        indicatorLayout = findViewById(R.id.indicator)
        getStartedButton = findViewById(R.id.getStartedButton)

        val fragments = arrayListOf(
            WelcomeFragment(),
            CameraAccessFragment(),
            OpenCameraFragment(),
            DirectPhoneFragment()
        )

        val adapter = OnboardingAdapter(this, fragments)
        viewPager.adapter = adapter

        setupIndicators(numPages)
        updateIndicators(0) // Set the first dot as active initially

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicators(position)
                getStartedButton.visibility = if (position == numPages - 1) View.VISIBLE else View.GONE
            }
        })

        getStartedButton.setOnClickListener {
            // Navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupIndicators(count: Int) {
        dots = Array(count) { ImageView(this) }
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(8, 0, 8, 0)

        for (i in dots.indices) {
            dots[i].apply {
                setImageDrawable(ContextCompat.getDrawable(
                    applicationContext,
                    R.drawable.indicator_dot_inactive
                ))
                this.layoutParams = layoutParams
            }
            indicatorLayout.addView(dots[i])
        }
    }

    private fun updateIndicators(position: Int) {
        for (i in dots.indices) {
            val drawableId = if (i == position) R.drawable.indicator_dot_active else R.drawable.indicator_dot_inactive
            dots[i].setImageDrawable(ContextCompat.getDrawable(applicationContext, drawableId))
        }
    }
}
