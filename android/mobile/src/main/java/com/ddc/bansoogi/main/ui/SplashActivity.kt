package com.ddc.bansoogi.main.ui

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
import com.ddc.bansoogi.landing.view.LandingActivity
import com.ddc.bansoogi.R
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.myInfo.data.entity.User
import io.realm.kotlin.ext.query

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

            val isUserEmpty = RealmManager.realm.query<User>().find().isEmpty()

            if (isUserEmpty) {
                startActivity(Intent(this, LandingActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
        }, splashScreenDelayTime)
    }
}