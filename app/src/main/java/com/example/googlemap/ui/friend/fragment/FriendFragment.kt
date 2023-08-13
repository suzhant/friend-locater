package com.example.googlemap.ui.friend.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.googlemap.R
import com.example.googlemap.adapter.FriendPagerAdapter
import com.example.googlemap.databinding.FragmentFriendBinding
import com.google.android.material.tabs.TabLayoutMediator


class FriendFragment : Fragment(), MenuItem.OnMenuItemClickListener {

    private val binding : FragmentFriendBinding by lazy {
        FragmentFriendBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.menu.findItem(R.id.search_view).setOnMenuItemClickListener(this)

        binding.viewPager2.apply {
            adapter =  FriendPagerAdapter(this@FriendFragment)
            isUserInputEnabled = false
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> tab.text = "Friend Request"
                1 -> tab.text = "Friends"
            }
        }.attach()

    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        findNavController().navigate(R.id.action_friendFragment_to_searchFragment)
        return false
    }

    override fun onDestroyView() {
        binding.viewPager2.adapter = null
        super.onDestroyView()
    }

}