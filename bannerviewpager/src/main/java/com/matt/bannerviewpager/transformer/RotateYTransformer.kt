package com.matt.bannerviewpager.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class RotateYTransformer (private val maxRotate: Float = 35f) : ViewPager2.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.pivotY = (view.height / 2).toFloat()
        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.rotationY = -1 * maxRotate
            view.pivotX = view.width.toFloat()
        } else if (position <= 1) { // [-1,1]
            // Modify the default slide transition to shrink the page as well
            view.rotationY = position * maxRotate

            //[0,-1]
            if (position < 0) {
                view.pivotX = view.width * (DEFAULT_CENTER + DEFAULT_CENTER * -position)
                view.pivotX = view.width.toFloat()
            } else { //[1,0]
                view.pivotX = view.width * DEFAULT_CENTER * (1 - position)
                view.pivotX = 0f
            }

            // Scale the page down (between MIN_SCALE and 1)
        } else {
            // (1,+Infinity]
            // This page is way off-screen to the right.
            view.rotationY = 1 * maxRotate
            view.pivotX = 0f
        }
    }

    companion object {
        private const val DEFAULT_CENTER = 0.5f
    }
}