package com.programminghut.realtime_object

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingAdapter(
    fa: FragmentActivity,
    private val fragments: List<Fragment>
) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}
