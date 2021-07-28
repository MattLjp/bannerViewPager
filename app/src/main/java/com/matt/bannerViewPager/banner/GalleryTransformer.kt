package com.matt.bannerViewPager.banner

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs


/**
 * @ Author : 廖健鹏
 * @ Date : 2021/7/27
 * @ e-mail : 329524627@qq.com
 * @ Description :
 */
class GalleryTransformer : ViewPager2.PageTransformer {
    /**
     * position参数指明给定页面相对于屏幕中心的位置。它是一个动态属性，会随着页面的滚动而改变。
     * 当一个页面（page)填充整个屏幕时，positoin值为0； 当一个页面（page)刚刚离开屏幕右(左）侧时，position值为1（-1）；
     * 当两个页面分别滚动到一半时，其中一个页面是-0.5，另一个页面是0.5。
     * 基于屏幕上页面的位置，通过诸如setAlpha()、setTranslationX
     * ()或setScaleY()方法来设置页面的属性，创建自定义的滑动动画。
     */
    override fun transformPage(view: View, position: Float) {
        val scale = 0.5f
        val scaleValue = 1 - abs(position) * scale
        view.scaleX = scaleValue
        view.scaleY = scaleValue
        view.alpha = scaleValue
        view.pivotX = view.width * (1 - position - (if (position > 0) 1 else -1) * 0.75f) * scale
        view.pivotY = view.height * (1 - scaleValue)
        view.elevation = if (position > -0.25 && position < 0.25) 1F else 0F
    }
}