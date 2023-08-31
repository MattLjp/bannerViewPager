package com.matt.bannerviewpager.banner

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.internal.ViewUtils.dpToPx

/**
 * Created by Liaojp on 2023/7/20
 */
class BannerViewPager<T> @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    private var mContext: Context = context
    private lateinit var mIndicatorLayout: LinearLayout
    private lateinit var mViewPager: ViewPager2
    private var mBannerPagerAdapter: BaseBannerAdapter<T, *>? = null
    private var mOnPageChangeCallback: OnPageChangeCallback? = null
    private var mCompositePageTransformer: CompositePageTransformer? = null
    private var mMarginPageTransformer: MarginPageTransformer? = null
    private var mOnPageClickListener: BaseBannerAdapter.OnPageClickListener<T>? = null

    //记录轮播图最后所在的位置
    private var lastPosition = 0
    private var listSize = 0


    /** 轮播定时器*/
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            val currentItem = mViewPager.currentItem
            if (isLooper) {
                fakeDrag(1)
            } else {
                if (currentItem == listSize - 1) {
                    mViewPager.endFakeDrag()
                    mViewPager.setCurrentItem(0, false)
                } else {
                    fakeDrag(1)
                }
            }
            mHandler.postDelayed(this, interval)
        }
    }

    /** 轮播间隔时间*/
    private var interval = 3000L

    /** 是否自动轮播*/
    private var isAutoPlay = false

    /** 是否是从左往右滚动*/
    private var isLeftToRight = true

    /** 滚动时长*/
    private var scrollDuration = 500L

    /** 是否循环*/
    private var isLooper = false

    /** 是否显示指示器*/
    private var isShowIndicator = false

    /** 页边距*/
    private var pageMargin = 0

    /** 一屏多页模式下两边页面显露出来的宽度*/
    private var revealWidth = 0

    /** 当前可见的任一侧的页数*/
    private var offscreenPageLimit = 2

    /** 指示器间隔*/
    private var indicatorMargin = dpToPx(5)

    /** 指示器间隔*/
    private var indicatorMarginBottom = dpToPx(10)


    /** 正常指示器图片*/
    private var normalImage = -1

    /** 选中指示器图片*/
    private var checkedImage = -1


    init {
        initView()
    }

    private val onPageChangeCallback: OnPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            val realPosition: Int = mBannerPagerAdapter!!.getRealPosition(position)
            mOnPageChangeCallback?.onPageScrolled(
                realPosition,
                positionOffset,
                positionOffsetPixels
            )
        }

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            val realPosition: Int = mBannerPagerAdapter!!.getRealPosition(position)
            mOnPageChangeCallback?.onPageSelected(realPosition)
            setIndicatorDots(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            mOnPageChangeCallback?.onPageScrollStateChanged(state)
        }
    }


    private fun initView() {
        mViewPager = ViewPager2(context).apply {
            val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutParams = params
        }
        addView(mViewPager)

        mIndicatorLayout = LinearLayout(context).apply {
            val params = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.addRule(ALIGN_PARENT_BOTTOM)
            params.setMargins(0, 0, 0, indicatorMarginBottom)
            layoutParams = params
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }
        addView(mIndicatorLayout)
        mCompositePageTransformer = CompositePageTransformer()
        mViewPager.setPageTransformer(mCompositePageTransformer)
    }

    /**
     * 初始化指示点
     *
     * @param list
     */
    private fun initIndicatorDots(list: List<T>) {
        mIndicatorLayout.removeAllViews()
        if (isShowIndicator && listSize > 1) {
            for (i in list.indices) {
                val imageView = ImageView(mContext)
                if (i == 0) imageView.setBackgroundResource(checkedImage)
                else imageView.setBackgroundResource(normalImage)
                //为指示点添加间距
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                when (i) {
                    0 -> layoutParams.setMargins(0, 0, indicatorMargin / 2, 0)
                    listSize - 1 -> layoutParams.setMargins(indicatorMargin / 2, 0, 0, 0)
                    else -> layoutParams.setMargins(indicatorMargin / 2, 0, indicatorMargin / 2, 0)
                }
                imageView.layoutParams = layoutParams
                //将指示点添加进容器
                mIndicatorLayout.addView(imageView)
            }
        }
    }

    private fun setIndicatorDots(position: Int) {
        if (isShowIndicator && listSize > 1) {
            //轮播时，改变指示点
            val current = position % listSize
            val last = lastPosition % listSize
            mIndicatorLayout.getChildAt(last).setBackgroundResource(normalImage)
            mIndicatorLayout.getChildAt(current).setBackgroundResource(checkedImage)
            lastPosition = position
        }
    }


    /**
     * 初始化ViewPager
     *
     */
    private fun setupViewPager() {
        if (mBannerPagerAdapter == null) {
            throw NullPointerException("You must set adapter for BannerViewPager")
        }
        val recyclerView = mViewPager.getChildAt(0) as RecyclerView
        if (mViewPager.orientation == ViewPager2.ORIENTATION_VERTICAL) {
            recyclerView.setPadding(
                mViewPager.paddingLeft,
                pageMargin + revealWidth,
                mViewPager.paddingRight,
                pageMargin + revealWidth
            )
        } else {
            recyclerView.setPadding(
                pageMargin + revealWidth,
                mViewPager.paddingTop,
                pageMargin + revealWidth,
                mViewPager.paddingBottom
            )
        }
        recyclerView.clipToPadding = false

        mBannerPagerAdapter!!.isCanLoop = isLooper
        mBannerPagerAdapter!!.pageClickListener = mOnPageClickListener
        mViewPager.adapter = mBannerPagerAdapter

        mViewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        mViewPager.registerOnPageChangeCallback(onPageChangeCallback)
        mViewPager.offscreenPageLimit = offscreenPageLimit
    }


    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                stopTimer()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE -> {
                startTimer()
            }

            else -> {
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun startTimer() {
        stopTimer()
        if (isAutoPlay && mBannerPagerAdapter != null && listSize > 1) {
            mHandler.postDelayed(runnable, interval)
        }
    }


    private fun stopTimer() {
        mHandler.removeCallbacks(runnable)
    }

    private fun fakeDrag(numberOfPages: Int) {
        val pxToDrag: Int = mViewPager.width - revealWidth * 2 - pageMargin * 2
        val animator = ValueAnimator.ofInt(0, pxToDrag)
        var previousValue = 0
        animator.addUpdateListener { valueAnimator ->
            val currentValue = valueAnimator.animatedValue as Int
            var currentPxToDrag: Float = (currentValue - previousValue).toFloat() * numberOfPages
            when {
                isLeftToRight -> {
                    currentPxToDrag *= -1
                }
            }
            mViewPager.fakeDragBy(currentPxToDrag)
            previousValue = currentValue
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                mViewPager.beginFakeDrag()
            }

            override fun onAnimationEnd(animation: Animator) {
                mViewPager.endFakeDrag()
            }

            override fun onAnimationCancel(animation: Animator) { /* Ignored */
                mViewPager.endFakeDrag()
            }

            override fun onAnimationRepeat(animation: Animator) { /* Ignored */
            }
        })
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.duration = scrollDuration
        animator.start()
    }

    /**
     * 设置自动轮播
     *
     * @param autoPlay
     * @return
     */
    fun setAutoPlay(autoPlay: Boolean): BannerViewPager<T> {
        isAutoPlay = autoPlay
        return this
    }

    /**
     * 设置滚动方向
     * @param [leftToRight] true为从左往右 false为从右往左
     * @return
     */
    fun setLeftToRight(leftToRight: Boolean): BannerViewPager<T> {
        isLeftToRight = leftToRight
        return this
    }

    /**
     * 设置滚动时长
     * @param duration
     * @return
     */
    fun seScrollDuration(duration: Long): BannerViewPager<T> {
        scrollDuration = duration
        return this
    }

    /**
     * 设置是否循环
     *
     * @param canLoop
     * @return
     */
    fun setCanLoop(canLoop: Boolean): BannerViewPager<T> {
        isLooper = canLoop
        return this
    }


    /**
     * Set adapter
     *
     * @param adapter
     * @return
     */
    fun setAdapter(adapter: BaseBannerAdapter<T, *>?): BannerViewPager<T> {
        mBannerPagerAdapter = adapter
        return this
    }


    /**
     * 设置轮播间隔时间
     *
     * @param int
     * @return
     */
    fun setInterval(int: Long): BannerViewPager<T> {
        interval = int
        return this
    }

    /**
     * 设置应保留到当前可见的任一侧的页数
     *
     * @param int
     * @return
     */
    fun setOffscreenPageLimit(int: Int): BannerViewPager<T> {
        offscreenPageLimit = int
        return this
    }

    /**
     * 设置是否显示指示器
     *
     */
    fun setCanShowIndicator(bool: Boolean): BannerViewPager<T> {
        isShowIndicator = bool
        return this
    }

    /**
     * 设置页面转换器
     *
     * @param transformer
     * @return
     */
    fun setPageTransformer(transformer: ViewPager2.PageTransformer): BannerViewPager<T> {
        mViewPager.setPageTransformer(transformer)
        return this
    }

    /**
     * [transformer] PageTransformer that will modify each page's animation properties
     */
    fun addPageTransformer(transformer: ViewPager2.PageTransformer): BannerViewPager<T> {
        mCompositePageTransformer?.addTransformer(transformer)
        return this
    }

    fun removeTransformer(transformer: ViewPager2.PageTransformer) {
        mCompositePageTransformer?.removeTransformer(transformer)
    }


    private fun removeMarginPageTransformer() {
        if (mMarginPageTransformer != null) {
            mCompositePageTransformer?.removeTransformer(mMarginPageTransformer!!)
        }
    }

    /**
     * 设置页边距
     *
     * @param margin page margin
     */
    fun setPageMargin(margin: Int): BannerViewPager<T> {
        pageMargin = margin
        removeMarginPageTransformer()
        mMarginPageTransformer = MarginPageTransformer(pageMargin)
        mCompositePageTransformer?.addTransformer(mMarginPageTransformer!!)
        return this
    }


    /**
     * 一屏多页模式下两边页面显露出来的宽度
     */
    fun setRevealWidth(int: Int): BannerViewPager<T> {
        revealWidth = int
        return this
    }

    /**
     * 设置项目点击监听器
     *
     * @param onPageClickListener item click listener
     */
    fun setOnPageClickListener(onPageClickListener: BaseBannerAdapter.OnPageClickListener<T>): BannerViewPager<T> {
        mOnPageClickListener = onPageClickListener
        return this
    }

    /**
     * 设置PageChangeCallback
     */
    fun setOnPageChangeListener(onPageClickListener: OnPageChangeCallback): BannerViewPager<T> {
        mOnPageChangeCallback = onPageClickListener
        return this
    }

    /**
     * 设置指示器间隔
     *
     */
    fun setIndicatorMargin(margin: Int): BannerViewPager<T> {
        indicatorMargin = margin
        return this
    }

    /**
     * 设置指示器底部距离
     *
     */
    fun indicatorMarginBottom(margin: Int): BannerViewPager<T> {
        indicatorMarginBottom = margin
        mIndicatorLayout.layoutParams = (mIndicatorLayout.layoutParams as LayoutParams).apply {
            setMargins(0, 0, 0, margin)
        }
        return this
    }

    /**
     * 设置指示器图片
     *
     */
    fun setIndicatorSliderColor(
        @DrawableRes normal: Int,
        @DrawableRes checked: Int
    ): BannerViewPager<T> {
        normalImage = normal
        checkedImage = checked
        return this
    }

    /**
     * 使用数据创建 BannerViewPager
     */
    fun create(data: List<T> = listOf()) {
        if (mBannerPagerAdapter == null) {
            throw NullPointerException("You must set adapter for BannerViewPager")
        }
        listSize = data.size
        mBannerPagerAdapter!!.setData(data)

        initIndicatorDots(data)
        setupViewPager()
        resetCurrentItem()
        startTimer()
    }

    fun refreshData(list: List<T>?) {
        val list = list ?: listOf()
        if (mBannerPagerAdapter == null) {
            throw NullPointerException("You must set adapter for BannerViewPager")
        }
        stopTimer()
        listSize = list.size
        mBannerPagerAdapter!!.setData(list)
        mBannerPagerAdapter!!.notifyDataSetChanged()

        initIndicatorDots(list)
        resetCurrentItem()
        startTimer()
    }

    fun getData(): List<T>? {
        return mBannerPagerAdapter?.getData()
    }

    fun getData(index: Int): T? {
        return mBannerPagerAdapter?.getData()?.getOrNull(index)
    }

    /**
     * Set current item
     *
     * @param item
     * @param smoothScroll
     */
    fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        if (isLooper && listSize > 1) {
            val currentItem = mViewPager.currentItem
            val realPosition: Int = mBannerPagerAdapter!!.getRealPosition(currentItem)
            if (currentItem != item) {
                if (item == 0 && realPosition == listSize - 1) {
                    mViewPager.setCurrentItem(currentItem + 1, smoothScroll)
                } else if (realPosition == 0 && item == listSize - 1) {
                    mViewPager.setCurrentItem(currentItem - 1, smoothScroll)
                } else {
                    mViewPager.setCurrentItem(currentItem + (item - realPosition), smoothScroll)
                }
            }
        } else {
            mViewPager.setCurrentItem(item, smoothScroll)
        }
    }


    private fun dpToPx(dip: Int): Int {
        return (0.5f + dip * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun resetCurrentItem() {
        mViewPager.endFakeDrag()
        if (listSize > 1 && isLooper) {
            lastPosition = Int.MAX_VALUE / 2 - ((Int.MAX_VALUE / 2) % listSize)
            mViewPager.setCurrentItem(lastPosition, false)
        } else {
            mViewPager.setCurrentItem(0, false)
        }
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTimer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTimer()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            startTimer()
        } else {
            stopTimer()
        }
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE && isVisible) {
            startTimer()
        } else {
            stopTimer()
        }
    }
}
