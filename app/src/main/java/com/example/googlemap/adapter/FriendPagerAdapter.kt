package com.example.googlemap.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.googlemap.ui.friend.fragment.FriendListFragment
import com.example.googlemap.ui.friend.fragment.FriendRequestFragment


class FriendPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return FriendRequestFragment()
            1 -> return FriendListFragment()
        }
        return FriendRequestFragment()
    }

    override fun getItemCount(): Int {
        return 2
    }

}
