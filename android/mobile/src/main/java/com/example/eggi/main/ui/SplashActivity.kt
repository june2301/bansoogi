package com.example.eggi.main.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.ImageLoader
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.eggi.LandingActivity
import com.example.eggi.R

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val splashScreenDelayTime: Long = 1500
        val imageView = findViewById<ImageView>(R.id.splashImageView)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()

        val request = ImageRequest.Builder(this)
            .data(R.drawable.bansoogi_walk)
            .target(imageView)
            .build()

        imageLoader.enqueue(request)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
        }, splashScreenDelayTime)
    }
}