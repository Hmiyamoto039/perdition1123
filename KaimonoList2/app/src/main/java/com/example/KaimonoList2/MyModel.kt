package com.example.KaimonoList2

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class MyModel: RealmObject(){
    @PrimaryKey
    var id :Long = 0
    var myKaimono2 :String = ""
}