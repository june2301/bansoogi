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
import com.ddc.bansoogi.collection.data.local.CollectionDataSource
import com.ddc.bansoogi.common.data.local.RealmManager
import com.ddc.bansoogi.myInfo.data.entity.User
import io.realm.kotlin.ext.query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        CoroutineScope(Dispatchers.IO).launch {
            CollectionDataSource().insertDummyCharactersWithUnlock()
        }

        Handler(Looper.getMainLooper()).postDelayed({

// MARK: 기존 로직
//////////////////////////////////////////////////////////////////////////////
            val isUserEmpty = RealmManager.realm.query<User>().find().isEmpty()

            if (isUserEmpty) {
                startActivity(Intent(this, LandingActivity::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
            finish()
//////////////////////////////////////////////////////////////////////////////

//MARK: DB를 모두 제거하여 초기상태 진입
//////////////////////////////////////////////////////////////////////////////
//            CoroutineScope(Dispatchers.Main).launch {
//                withContext(Dispatchers.IO) {
//                    RealmManager.realm.write {
//                        deleteAll()
//                    }
//                }
//
//                val isUserEmpty = RealmManager.realm.query<User>().find().isEmpty()
//
//                val nextIntent = if (isUserEmpty) {
//                    Intent(applicationContext, LandingActivity::class.java)
//                } else {
//                    Intent(applicationContext, MainActivity::class.java)
//                }
//
//                startActivity(nextIntent)
//                finish()
//            }
//////////////////////////////////////////////////////////////////////////////
        }, splashScreenDelayTime)
    }
}