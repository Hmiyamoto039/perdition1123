package com.example.KaimonoList2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.realm.Realm
import io.realm.RealmResults

class RecyclerAdapter(realmResults: RealmResults<MyModel>): RecyclerView.Adapter<ViewHolderItem>() {

    private lateinit var realm: Realm
    private val rResults:RealmResults<MyModel> = realmResults


    //1行だけのview生成
    //inflateなんやねん？
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderItem {
        val itemXml = LayoutInflater.from(parent.context)
            .inflate(R.layout.one_layout,parent,false)
        return ViewHolderItem(itemXml)//RecyclerAdapter受け渡し
    }

    //potision番目のデータ取得
    override fun onBindViewHolder(holder: ViewHolderItem,position: Int) {

        val myModel = rResults[position]//何番目のリストか

        holder.itemTv.text = myModel?.myKaimono2.toString()//リスト(myKaimono)の中の要素を指定して代入

        holder.itemView.setOnClickListener {
            if (myModel != null) {
                deleteItem(myModel.id)
            }
            }
        }

    override fun getItemCount(): Int {
        return rResults.size
    }

    private fun deleteItem(id: kotlin.Long){
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction {
            val itemToDelete = it.where(MyModel::class.java)
                .equalTo("id",id).findFirst()
            itemToDelete?.deleteFromRealm()
        }
        notifyDataSetChanged()
    }

}