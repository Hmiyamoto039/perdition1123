package com.example.KaimonoList2

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.realm.Realm
import io.realm.Sort
import io.realm.kotlin.createObject
import io.realm.kotlin.where

class MainActivity : AppCompatActivity() {

    //realmの変数を用意
    private lateinit var realm: Realm

    //追加リスト、RecyclerView、アダプター用意
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter : RecyclerAdapter
    private lateinit var layoutManager: LinearLayoutManager



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        //recyclerview取得、アダプターにセット、レイアウトにセット
        //recyclerView = findViewById(R.id.rv)
        //recyclerView.adapter = recyclerAdapter
        //recyclerView.layoutManager = LinearLayoutManager(this)

        //realmに登録された全件数を探す
       // val realmResults:List<MyModel> = realm.where(MyModel::class.java).findAll()

        //追加ボタンを呼び出す
        val addBtn: View = findViewById(R.id.addBtn)
        //realmのインスタンス
        realm = Realm.getDefaultInstance()


        //フローティングボタン押したときの処理
        addBtn.setOnClickListener {
            val editText = AppCompatEditText(this)
            AlertDialog.Builder(this)
                .setTitle("何を買う？")
                .setView(editText)
                //追加を押したら
                .setPositiveButton("買う") { dialog, _ ->
                    var kList: String = ""
                    if (!editText.text.isNullOrEmpty()) {
                        kList = editText.text.toString()

                        //DBに書き込み
                        realm.executeTransaction {
                            //現在のidを取得
                            val currentId = realm.where<MyModel>().max("id")
                            //最高値に1を追加(最高値が0なら1にする)
                            //エルビス演算子
                            val nextId = (currentId?.toLong() ?: 0L) + 1L

                            //モデルクラスに値をセット
                            val myModel = realm.createObject<MyModel>(nextId)
                            myModel.myKaimono2 = kList
                        }
                        recyclerAdapter.notifyDataSetChanged()
                        dialog.dismiss()
                    }
                }
                .setNegativeButton("買わない"){dialog,_ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        val realmResults = realm.where(MyModel::class.java)
            .findAll().sort("id",Sort.ASCENDING)

        recyclerView = findViewById(R.id.rv)
        recyclerAdapter = RecyclerAdapter(realmResults)
        recyclerView.adapter = recyclerAdapter

        layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

    }

    //realmを閉じる
    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}