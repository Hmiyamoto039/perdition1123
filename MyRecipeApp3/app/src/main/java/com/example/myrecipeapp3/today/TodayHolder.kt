package com.example.myrecipeapp3.today

import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myrecipeapp3.R

class TodayHolder(itemView: View):RecyclerView.ViewHolder(itemView) {

    var title : TextView = itemView.findViewById(R.id.todayTitle)
    var toDayCheck : CheckBox = itemView.findViewById(R.id.toDayCheck)
    var deleteBtn : ImageView = itemView.findViewById(R.id.deleteBtn)
    var recipeReEdit : ImageView = itemView.findViewById(R.id.recipeReEdit2)

}