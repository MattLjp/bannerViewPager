package com.matt.bannerviewpager.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class RotateDownPageTransformer(private val maxRotate: Float = 15f) : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        if (position < -1) {
            // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.rotation = maxRotate * -1
            view.pivotX = view.width.toFloat()
            view.pivotY = view.height.toFloat()
        } else if (position <= 1) { // [-1,1]
            if (position < 0) { //[0，-1]
                view.pivotX = view.width * (DEFAULT_CENTER + DEFAULT_CENTER * -position)
                view.pivotY = view.height.toFloat()
                view.rotation = maxRotate * position
            } else { //[1,0]
                view.pivotX = view.width * DEFAULT_CENTER * (1 - position)
                view.pivotY = view.height.toFloat()
                view.rotation = maxRotate * position
            }
        } else {
            // (1,+Infinity]
            // This page is way off-screen to the right.
            view.rotation = maxRotate
            view.pivotX = 0f
            view.pivotY = view.height.toFloat()
        }
    }

    companion object {
        private const val DEFAULT_CENTER = 0.5f
    }
}