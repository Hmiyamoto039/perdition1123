package com.example.myrecipeapp3.today

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myrecipeapp3.R
import com.example.myrecipeapp3.realm.Recipe
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort


class TodayFragment : Fragment() {

    private lateinit var realm : Realm

    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: TodayAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var rResults : RealmResults<Recipe>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.today_fragment,container,false)

        realm = Realm.getDefaultInstance()

        rResults = realm.where(Recipe::class.java)
            .equalTo("toDayCheck",true)
            .findAll().sort("id",Sort.DESCENDING)

        recyclerView = view.findViewById(R.id.todayRv)
        recyclerAdapter = TodayAdapter(rResults)
        recyclerView.adapter = recyclerAdapter

        layoutManager = LinearLayoutManager(view.context)
        recyclerView.layoutManager = layoutManager

        return view
    }

    override fun onResume() {
        super.onResume()
        recyclerAdapter = TodayAdapter(rResults)
        recyclerView.adapter = recyclerAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        realm.close()
    }
}