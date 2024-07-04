package com.example.myrecipeapp3.recipe

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.myrecipeapp3.R
import com.example.myrecipeapp3.RecipeDetailActivity
import com.example.myrecipeapp3.RecipeEditActivity
import com.example.myrecipeapp3.realm.Recipe
import io.realm.Realm
import io.realm.RealmResults

class RecipeAdapter(private var rResults: RealmResults<Recipe>) : RecyclerView.Adapter<RecipeHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeHolder {
        val itemXml = LayoutInflater.from(parent.context)
            .inflate(R.layout.recipe_one,parent,false)
        return RecipeHolder(itemXml)
    }

    override fun getItemCount(): Int {
        return rResults.size
    }

    override fun onBindViewHolder(holder: RecipeHolder, position: Int) {
        val recipePosition = rResults[position]

        //タイトルの取得,タイトルをタッチした時の処理
        holder.title.text = recipePosition?.recipeTitle.toString()
        holder.title.setOnClickListener {
            val intent = Intent(it.context,RecipeDetailActivity::class.java)
            intent.putExtra("ID",recipePosition?.id)
            it.context.startActivity(intent)
        }

        holder.toDayCheck.isChecked = recipePosition?.toDayCheck ?: false
        holder.toDayCheck.setOnCheckedChangeListener { _, isChecked ->
            recipePosition?.let{
                updateTodayStatus(it.id,isChecked)
            }
        }

        holder.deleteBtn.setOnClickListener {
            AlertDialog.Builder(it.context)
                .setTitle("レシピを削除しますか？")
                .setPositiveButton("削除"){dialog,_ ->
                    if(recipePosition != null){
                        deleteRecipe(recipePosition.id)
                        notifyItemRemoved(position)
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("キャンセル"){dialog,_ ->
                    dialog.dismiss()
                }
                .show()
        }

        holder.recipeReEdit.setOnClickListener {
            val intent = Intent(it.context, RecipeEditActivity::class.java)
            intent.putExtra("ID",recipePosition?.id)
            it.context.startActivity(intent)
        }

    }

    fun updateData(newResults :RealmResults<Recipe>){
        rResults = newResults
        notifyDataSetChanged()
    }

    private fun deleteRecipe(id :Long){
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val recipeToDelete = it.where(Recipe::class.java)
                .equalTo("id",id).findFirst()
            recipeToDelete?.deleteFromRealm()
        }
        realm.close()
    }

    private fun updateTodayStatus(id: Long,isTodayCheck:Boolean){
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val recipeToUpdate = it.where(Recipe::class.java)
                .equalTo("id",id).findFirst()
            recipeToUpdate?.toDayCheck = isTodayCheck
        }
        realm.close()
    }


}