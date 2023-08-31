package com.matt.bannerviewpager.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class RotateUpPageTransformer(private val maxRotate: Float = 15f) : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.rotation = maxRotate
            view.pivotX = view.width.toFloat()
            view.pivotY = 0f
        } else if (position <= 1) {  // a页滑动至b页 ； a页从 0.0 ~ -1 ；b页从1 ~ 0.0
            // [-1,1]
            // Modify the default slide transition to shrink the page as well
            if (position < 0) { //[0，-1]
                view.pivotX = view.width * (DEFAULT_CENTER + DEFAULT_CENTER * -position)
                view.pivotY = 0f
                view.rotation = -maxRotate * position
            } else { //[1,0]
                view.pivotX = view.width * DEFAULT_CENTER * (1 - position)
                view.pivotY = 0f
                view.rotation = -maxRotate * position
            }
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            // ViewHelper.setRotation(view, ROT_MAX);
            view.rotation = -maxRotate
            view.pivotX = 0f
            view.pivotY = 0f
        }
    }

    companion object {
        private const val DEFAULT_CENTER = 0.5f
    }
}