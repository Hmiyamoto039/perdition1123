package com.example.KaimonoList2

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ViewHolderItem(itemView:View) : RecyclerView.ViewHolder(itemView) {

    //xmlから指定のidを見つける
    var itemTv:TextView = itemView.findViewById(R.id.tv)

}