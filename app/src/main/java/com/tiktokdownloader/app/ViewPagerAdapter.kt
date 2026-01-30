package com.tiktokdownloader.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    activity: FragmentActivity,
    private val platforms: List<String>
) : FragmentStateAdapter(activity) {
    
    override fun getItemCount(): Int = platforms.size
    
    override fun createFragment(position: Int): Fragment {
        return DownloadFragment.newInstance(platforms[position])
    }
}
