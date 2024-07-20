package com.example.gcalendartest

import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.gcalendartest.alltask.AllTaskFragment
import com.example.gcalendartest.today.TodayTaskFragment

class ViewPagerHolder(fa:FragmentActivity):FragmentStateAdapter(fa) {

    private val fragments = mutableListOf<Fragment>()

    init {
        fragments.add(TodayTaskFragment())
        fragments.add(AllTaskFragment())
    }

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun getFragment(position: Int): Fragment?{
        return if(position in fragments.indices) fragments[position] else null
    }

    fun getFragmentPosition(fragmentClass: Class<out Fragment>): Int {
        return fragments.indexOfFirst { fragmentClass.isInstance(it) }
    }

}