package com.matt.bannerviewpager.transformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2


class ScaleTransformer(private val minScale: Float = 0.85f) : ViewPager2.PageTransformer {

    /**
     * position参数指明给定页面相对于屏幕中心的位置。它是一个动态属性，会随着页面的滚动而改变。
     * 当一个页面（page)填充整个屏幕时，position值为0； 当一个页面（page)刚刚离开屏幕右(左）侧时，position值为1（-1）；
     * 当两个页面分别滚动到一半时，其中一个页面是-0.5，另一个页面是0.5。
     * 基于屏幕上页面的位置，通过诸如setAlpha()、setTranslationX()或
     * setScaleY()方法来设置页面的属性，创建自定义的滑动动画。
     */
    override fun transformPage(view: View, position: Float) {
        val pageWidth = view.width
        val pageHeight = view.height

        view.pivotY = (pageHeight / 2).toFloat()
        view.pivotX = (pageWidth / 2).toFloat()
        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.scaleX = minScale
            view.scaleY = minScale
            view.pivotX = pageWidth.toFloat()
        } else if (position <= 1) { // [-1,1]
            // Modify the default slide transition to shrink the page as well
            if (position < 0) //1-2:1[0,-1] ;2-1:1[-1,0]
            {
                val scaleFactor: Float = (1 + position) * (1 - minScale) + minScale
                view.scaleX = scaleFactor
                view.scaleY = scaleFactor
                view.pivotX = pageWidth * (DEFAULT_CENTER + DEFAULT_CENTER * -position)
            } else  //1-2:2[1,0] ;2-1:2[0,1]
            {
                val scaleFactor: Float = (1 - position) * (1 - minScale) + minScale
                view.scaleX = scaleFactor
                view.scaleY = scaleFactor
                view.pivotX = pageWidth * ((1 - position) * DEFAULT_CENTER)
            }
        } else { // (1,+Infinity]
            view.pivotX = 0f
            view.scaleX = minScale
            view.scaleY = minScale
        }
    }

    companion object {
        private const val DEFAULT_CENTER = 0.5f
    }

}