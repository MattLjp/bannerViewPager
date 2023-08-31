package com.matt.bannerviewpager.transformer

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs


class MZScaleInTransformer(private val minScale: Float = 0.85f) : ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
        val viewPager = requireViewPager(view)
        val paddingLeft = viewPager.paddingLeft.toFloat()
        val paddingRight = viewPager.paddingRight.toFloat()
        val width = viewPager.measuredWidth.toFloat()
        val offsetPosition = paddingLeft / (width - paddingLeft - paddingRight)
        val currentPos = position - offsetPosition
        var reduceX = 0f
        val itemWidth = view.width.toFloat()
        //由于左右边的缩小而减小的x的大小的一半
        reduceX = (1.0f - minScale) * itemWidth / 2.0f
        if (currentPos <= -1.0f) {
            view.translationX = reduceX
            view.scaleX = minScale
            view.scaleY = minScale
        } else if (currentPos <= 1.0) {
            val scale = (1.0f - minScale) * abs(1.0f - abs(currentPos))
            val translationX = currentPos * -reduceX
            if (currentPos <= -0.5) { //两个view中间的临界，这时两个view在同一层，左侧View需要往X轴正方向移动覆盖的值()
                view.translationX = translationX + abs(abs(currentPos) - 0.5f) / 0.5f
            } else if (currentPos <= 0.0f) {
                view.translationX = translationX
            } else if (currentPos >= 0.5) { //两个view中间的临界，这时两个view在同一层
                view.translationX = translationX - abs(abs(currentPos) - 0.5f) / 0.5f
            } else {
                view.translationX = translationX
            }
            view.scaleX = scale + minScale
            view.scaleY = scale + minScale
        } else {
            view.scaleX = minScale
            view.scaleY = minScale
            view.translationX = -reduceX
        }
    }

    private fun requireViewPager(page: View): ViewPager2 {
        val parent = page.parent
        val parentParent = parent.parent
        if (parent is RecyclerView && parentParent is ViewPager2) {
            return parentParent
        }
        throw IllegalStateException(
            "Expected the page view to be managed by a ViewPager2 instance."
        )
    }
}