package com.example.myrecipeapp3

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myrecipeapp3.recipe.RecipeFragment
import com.example.myrecipeapp3.today.TodayFragment

class ViewPagerAdapter (fa:FragmentActivity):FragmentStateAdapter(fa){

    private val fragments = listOf(
        TodayFragment(),
        RecipeFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

}