package com.example.myrecipeapp3.recipe

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrecipeapp3.R

class RecipeHolder(itemView : View):RecyclerView.ViewHolder(itemView){

    //レイアウトをviewholderに紐づけ
    var title : TextView = itemView.findViewById(R.id.recipeTitle)
    var toDayCheck : CheckBox = itemView.findViewById(R.id.toDayCheck)
    var deleteBtn :ImageView = itemView.findViewById(R.id.deleteBtn)
    var recipeReEdit :ImageView = itemView.findViewById(R.id.recipeReEdit)
}