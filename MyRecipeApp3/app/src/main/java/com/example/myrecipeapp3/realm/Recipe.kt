package com.example.myrecipeapp3.realm


import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class Recipe : RealmObject() {
    @PrimaryKey
    var id :Long = 0
    var recipeTitle :String = ""
    var recipeDetail :String = ""
    var recipeTag :String =""
    var toDayCheck :Boolean = false
}