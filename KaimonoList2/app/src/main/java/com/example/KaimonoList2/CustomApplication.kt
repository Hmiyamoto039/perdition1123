package com.example.KaimonoList2

import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

//初期化と構築
class CustomApplication :Application(){
    override fun onCreate() {
        super.onCreate()
        Realm.init(this)

        //Configuration 構築
        val config = RealmConfiguration.Builder()
            .allowWritesOnUiThread(true)
            .allowQueriesOnUiThread(true)
            .build()
        Realm.setDefaultConfiguration(config)
    }

}