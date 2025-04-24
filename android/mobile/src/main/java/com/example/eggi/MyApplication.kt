package com.example.eggi

import android.app.Application
import io.realm.kotlin.Realm

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 또는 단순히 클래스를 참조하여 필요할 때 초기화되도록 할 수도 있음
        // 실제 사용 시점에 초기화됨
    }

    override fun onTerminate() {
        super.onTerminate()
    }
}