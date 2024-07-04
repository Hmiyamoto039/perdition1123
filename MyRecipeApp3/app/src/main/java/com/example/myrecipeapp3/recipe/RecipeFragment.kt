package com.example.myrecipeapp3.recipe

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrecipeapp3.R
import com.example.myrecipeapp3.realm.Recipe
import io.realm.Case
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RecipeFragment : Fragment() {

    private lateinit var realm : Realm

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: RecipeAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var rResults : RealmResults<Recipe>

    private val REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.recipe_fragment,container,false)

        realm = Realm.getDefaultInstance()

        //画面のボタンを取得
        val resetBtn :TextView = view.findViewById(R.id.rFgTitle)
        val searchBtn :ImageView = view.findViewById(R.id.searchBtn)
        val exportBtn :ImageView = view.findViewById(R.id.exportBtn)

        //検索リセットボタン
        resetBtn.setOnClickListener {
            rResults = realm.where(Recipe::class.java)
                .findAll().sort("id",Sort.DESCENDING)
            recyclerAdapter.updateData(rResults)
        }

        //検索用レイアウト

        //検索ボタン
        searchBtn.setOnClickListener {
            val editText = AppCompatEditText(view.context)
            val titleSText =AppCompatEditText(view.context).apply { hint = "タイトル" }
            val tagSText = AppCompatEditText(view.context).apply {hint = "タグ"}

            val searchLayout = LinearLayout(view.context).apply {
                orientation = LinearLayout.VERTICAL
                addView(titleSText)
                addView(tagSText)
            }
            AlertDialog.Builder(view.context)
                .setTitle("レシピを検索")
                .setView(searchLayout)
                .setPositiveButton("検索"){dialog,_ ->

                    val title = titleSText.text.toString()
                    val tag = tagSText.text.toString()

                    var query = realm.where(Recipe::class.java)

                    if(title.isNotEmpty()){
                        query = query.contains("recipeTitle",title,Case.INSENSITIVE)
                    }
                    if(tag.isNotEmpty()){
                        query = query.and().contains("recipeTag",tag,Case.INSENSITIVE)
                    }

                    rResults = query.findAll().sort("id",Sort.DESCENDING)
                    recyclerAdapter.updateData(rResults)

                    dialog.dismiss()
                }
                .setNegativeButton("キャンセル"){dialog,_ ->
                    dialog.dismiss()
                }
                .create()
                .show()

           /* AlertDialog.Builder(view.context)
                .setTitle("レシピを検索")
                .setView(editText)
                .setPositiveButton("検索"){dialog,_ ->
                    var searchText :String =""
                    if(!editText.text.isNullOrEmpty()){
                        searchText = editText.text.toString()
                        rResults = realm.where(Recipe::class.java)
                            .contains("recipeTitle",searchText,Case.INSENSITIVE)
                            .or()
                            .contains("recipeTag",searchText,Case.INSENSITIVE)
                            .findAll().sort("id",Sort.DESCENDING)
                        recyclerAdapter.updateData(rResults)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("キャンセル"){dialog,_ ->
                    dialog.dismiss()
                }
                .create()
                .show()*/
        }

        //エクスポートボタン
        exportBtn.setOnClickListener {
            AlertDialog.Builder(view.context)
                .setTitle("データ共有")
                .setPositiveButton("インポート"){dialog,_->
                    importRealmData()
                    dialog.dismiss()
                }
                .setNegativeButton("エクスポート"){dialog,_->
                    checkPermission()
                    dialog.dismiss()
                }
                .show()
        }

        //リスト情報
        rResults = realm.where(Recipe::class.java)
            .findAll().sort("id",Sort.DESCENDING)

        //画面作成
        recyclerView = view.findViewById(R.id.recipeRv)
        recyclerAdapter = RecipeAdapter(rResults)
        recyclerView.adapter = recyclerAdapter

        layoutManager = LinearLayoutManager(view.context)
        recyclerView.layoutManager = layoutManager

        return view
    }

    //インポート処理
    private fun importRealmData(){
        try {
            val realm = Realm.getDefaultInstance()
            val importFile = File(requireContext().getExternalFilesDir(null),"recipeExport.realm")

            if(!importFile.exists()){
                Toast.makeText(requireContext(),"インポートファイルが見つかりません",Toast.LENGTH_SHORT)
                    .show()
                return
            }

            realm.beginTransaction()
            realm.deleteAll()
            realm.commitTransaction()

            val inputStream = FileInputStream(importFile)
            val outputStream = FileOutputStream(File(realm.path))

            val buffer = ByteArray(1024)
            var length: Int

            while(inputStream.read(buffer).also { length = it } > 0){
                outputStream.write(buffer,0,length)
            }
            inputStream.close()
            outputStream.close()

            Toast.makeText(requireContext(),"レシピデータがインポートされました",Toast.LENGTH_SHORT)
                .show()
            realm.close()


        }catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(requireContext(),"インポート中にエラーが発生しました",Toast.LENGTH_SHORT)
                .show()
        }
    }
    //インポート処理ここまで

    //エクスポート処理
    private fun checkPermission(){
        if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),REQUEST_CODE)
        }else{
            exportRealmData()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == REQUEST_CODE){
            if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                exportRealmData()
            }else{
                Toast.makeText(requireContext(),"権限が拒否されました",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportRealmData(){
        try {
            val realm = Realm.getDefaultInstance()
            val realmPath = realm.path

            val realmFile = File(realmPath)
            val exportFile = File(requireContext().getExternalFilesDir(null),"recipeExport.realm")

            if(exportFile.exists()){
                exportFile.delete()
            }

            if (realmFile.exists()){
                val inputStream = FileInputStream(realmFile)
                val outputStream = FileOutputStream(exportFile)

                val buffer = ByteArray(1024)
                var length: Int

                while (inputStream.read(buffer).also { length = it } > 0 ){
                    outputStream.write(buffer,0,length)
                }
                inputStream.close()
                outputStream.close()

                Toast.makeText(requireContext(),"レシピデータがエクスポートされました",Toast.LENGTH_SHORT)
                    .show()
            }
            realm.close()
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(requireContext(),"エクスポート中にエラーが発生しました",Toast.LENGTH_SHORT)
                .show()
        }
    }
    //ここまでエクスポート処理

    //RecyclerView更新
    private fun updateRecyclerView(){

        recyclerView ?: return

       if(!recyclerView.isComputingLayout && !recyclerView.isAnimating){
           recyclerAdapter.updateData(rResults)
           recyclerAdapter.notifyDataSetChanged()
       }else{
           recyclerView.post{
               updateRecyclerView()
           }
       }
    }

 /*   override fun onStart() {
        super.onStart()
        recyclerAdapter = RecipeAdapter(rResults)
        recyclerView.adapter = recyclerAdapter
    }*/

    //画面更新
    override fun onResume() {
        super.onResume()
        recyclerAdapter = RecipeAdapter(rResults)
        recyclerView.adapter = recyclerAdapter
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}