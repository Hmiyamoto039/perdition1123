package com.example.myrecipeapp3

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myrecipeapp3.realm.Recipe
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class RecipeEditActivity : AppCompatActivity() {

    private lateinit var realm : Realm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_edit)

        realm = Realm.getDefaultInstance()

        val titleEdit : TextView = findViewById(R.id.titleEdit)
        val detailEdit : TextView = findViewById(R.id.detailEdit)
        val tagEdit :TextView = findViewById(R.id.tagEdit)

        val saveBtn :Button = findViewById(R.id.saveBtn)
        val cancelBtn :Button = findViewById(R.id.cancelBtn)

        val getId = intent.getLongExtra("ID",0L)

        if(getId>0){
            val recipe  = realm.where<Recipe>()
                .equalTo("id",getId).findFirst()
            titleEdit.text = recipe?.recipeTitle.toString()
            detailEdit.text = recipe?.recipeDetail.toString()
            tagEdit.text = recipe?.recipeTag.toString()
        }

        saveBtn.setOnClickListener {
            var title :String=""
            var detail :String=""
            var tag :String=""

            if(!titleEdit.text.isNullOrEmpty()){
                title = titleEdit.text.toString()
                detail = detailEdit.text.toString()
                tag = tagEdit.text.toString()
            }

            if(getId == 0L){
                realm.executeTransaction {
                    val currentId = realm.where<Recipe>().max("id")
                    val nextId = (currentId?.toLong() ?: 0L) + 1L

                    val recipe = realm.createObject<Recipe>(nextId)
                    recipe.recipeTitle = title
                    recipe.recipeDetail = detail
                    recipe.recipeTag = tag

                }
                finish()
            }else{
                realm.executeTransaction {
                    val myRecipe = realm.where<Recipe>()
                        .equalTo("id",getId).findFirst()
                    myRecipe?.recipeTitle = title
                    myRecipe?.recipeDetail = detail
                    myRecipe?.recipeTag = tag

                }
                finish()
            }

        }

        cancelBtn.setOnClickListener{
            finish()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}