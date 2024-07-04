package com.example.myrecipeapp3

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myrecipeapp3.realm.Recipe
import io.realm.Realm
import io.realm.kotlin.where

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var realm: Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_detail)

        realm = Realm.getDefaultInstance()

        val getId = intent.getLongExtra("ID",0L)

        val recipeTitle :TextView = findViewById(R.id.recipeDetailTitle)
        val recipeDetail :TextView = findViewById(R.id.recipeDetail)
        recipeDetail.movementMethod = ScrollingMovementMethod()

        val recipeResult = realm.where<Recipe>()
            .equalTo("id",getId).findFirst()

        recipeTitle.text = recipeResult?.recipeTitle
        recipeDetail.text = recipeResult?.recipeDetail

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}