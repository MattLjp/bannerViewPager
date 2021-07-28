package com.matt.bannerViewPager.banner

import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.matt.bannerViewPager.R


/**
 * @ Author : 廖健鹏
 * @ Date : 2021/7/27
 * @ e-mail : 329524627@qq.com
 * @ Description : 自定义轮播图
 */

class BannerViewPager<T> : RelativeLayout, LifecycleObserver {

    private var mContext: Context = context
    private lateinit var mIndicatorLayout: LinearLayout
    private lateinit var mViewPager: ViewPager2
    private var mBannerPagerAdapter: BaseBannerAdapter<T, *>? = null
    private val onPageChangeCallback: OnPageChangeCallback? = null
    private var mCompositePageTransformer: CompositePageTransformer? = null
    private var mMarginPageTransformer: MarginPageTransformer? = null
    private var mOnPageClickListener: BaseBannerAdapter.OnPageClickListener? = null

    //记录轮播图最后所在的位置
    private var lastPosition = 0
    private var listSize = 0


    /**
     * 轮播定时器
     */
    private val mHandler: Handler = Handler()
    private val runnable: Runnable = object : Runnable {
        override fun run() {
            val currentItem = mViewPager.currentItem
            if (isLooper) {
                mViewPager.currentItem = currentItem + 1
            } else {
                if (currentItem == listSize - 1) {
                    mViewPager.setCurrentItem(0, false)
                } else {
                    mViewPager.currentItem = currentItem + 1
                }
            }
            mHandler.postDelayed(this, interval)
        }
    }

    /**
     * 轮播间隔时间
     */
    private var interval = 3000L

    /**
     * 是否自动轮播
     */
    private var isAutoPlay = false

    /**
     * 是否循环
     */
    private var isLooper = false

    /**
     * 是否显示指示器
     */
    private var isShowIndicator = false

    /**
     * 页边距
     */
    private var pageMargin = 0

    /**
     * 一屏多页模式下两边页面显露出来的宽度
     */
    private var revealWidth = -1


    /**
     * 当前可见的任一侧的页数
     */
    private var offscreenPageLimit = 3

    /**
     * 指示器间隔
     */
    private var indicatorMargin = dpToPx(5)

    /**
     * 正常指示器图片
     */
    private var normalImage = R.drawable.shape_dot

    /**
     * 选中指示器图片
     */
    private var checkedImage = R.drawable.shape_dot_selected


    private val mOnPageChangeCallback: OnPageChangeCallback = object : OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            val realPosition: Int = mBannerPagerAdapter!!.getRealPosition(position)
            onPageChangeCallback?.onPageScrolled(realPosition, positionOffset, positionOffsetPixels)
        }

        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            val realPosition: Int = mBannerPagerAdapter!!.getRealPosition(position)
            onPageChangeCallback?.onPageSelected(realPosition)
            setIndicatorDots(position)
        }

        override fun onPageScrollStateChanged(state: Int) {
            super.onPageScrollStateChanged(state)
            onPageChangeCallback?.onPageScrollStateChanged(state)
        }
    }

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView()
        mCompositePageTransformer = CompositePageTransformer()
        mViewPager.setPageTransformer(mCompositePageTransformer)
    }

    private fun initView() {
        inflate(context, R.layout.bvp_layout, this)
        mViewPager = findViewById(R.id.vp_main)
        mIndicatorLayout = findViewById(R.id.bvp_layout_indicator)
    }


    private fun initBannerData(list: List<T>) {
        if (list.isNotEmpty()) {
            initIndicatorDots(list)
            setupViewPager(list)
        }
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
                layoutParams.setMargins(
                    indicatorMargin,
                    indicatorMargin,
                    indicatorMargin,
                    indicatorMargin
                )
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
            mIndicatorLayout.getChildAt(current).setBackgroundResource(checkedImage)
            mIndicatorLayout.getChildAt(last).setBackgroundResource(normalImage)
            lastPosition = position
        }
    }


    /**
     * 初始化ViewPager
     *
     * @param list
     */
    private fun setupViewPager(list: List<T>) {
        if (mBannerPagerAdapter == null) {
            throw NullPointerException("You must set adapter for BannerViewPager")
        }

        if (revealWidth != -1) {
            val recyclerView = mViewPager.getChildAt(0) as RecyclerView
            recyclerView.setPadding(pageMargin + revealWidth, 0, pageMargin + revealWidth, 0)
            recyclerView.clipToPadding = false
        }

        mBannerPagerAdapter!!.isCanLoop = isLooper
        mBannerPagerAdapter!!.pageClickListener = mOnPageClickListener
        mViewPager.adapter = mBannerPagerAdapter
        resetCurrentItem()

        mViewPager.unregisterOnPageChangeCallback(mOnPageChangeCallback)
        mViewPager.registerOnPageChangeCallback(mOnPageChangeCallback)
        mViewPager.offscreenPageLimit = offscreenPageLimit
        startTimer()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart(owner: LifecycleOwner?) {
        startTimer()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop(owner: LifecycleOwner?) {
        stopTimer()
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
        if (isAutoPlay && mBannerPagerAdapter != null && listSize > 1) {
            stopTimer()
            mHandler.postDelayed(runnable, interval)
        }
    }


    private fun stopTimer() {
        mHandler.removeCallbacks(runnable)
    }


    /**
     * 如果开启自动轮询，必须设置生命周期，不然会内存泄漏
     * @param lifecycleRegistry Lifecycle
     * @return BannerViewPager<T>
     */
    fun setLifecycleRegistry(lifecycleRegistry: Lifecycle): BannerViewPager<T> {
        lifecycleRegistry.addObserver(this)
        return this
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
    fun setInterval(int: Int): BannerViewPager<T> {
        interval = int * 1000L
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


    fun removeMarginPageTransformer() {
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
        pageMargin = dpToPx(margin)
        removeMarginPageTransformer()
        mMarginPageTransformer = MarginPageTransformer(pageMargin)
        mCompositePageTransformer?.addTransformer(mMarginPageTransformer!!)
        return this
    }


    /**
     * 一屏多页模式下两边页面显露出来的宽度
     */
    fun setRevealWidth(int: Int): BannerViewPager<T> {
        revealWidth = dpToPx(int)
        return this
    }

    /**
     * 设置项目点击监听器
     *
     * @param onPageClickListener item click listener
     */
    fun setOnPageClickListener(onPageClickListener: BaseBannerAdapter.OnPageClickListener): BannerViewPager<T> {
        mOnPageClickListener = onPageClickListener
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
    fun create(data: List<T>) {
        if (mBannerPagerAdapter == null) {
            throw NullPointerException("You must set adapter for BannerViewPager")
        }
        listSize = data.size
        mBannerPagerAdapter!!.setData(data)
        initBannerData(data)
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


    fun refreshData(list: List<T>) {
        if (mBannerPagerAdapter != null && list.isNotEmpty()) {
            stopTimer()
            listSize = list.size
            mBannerPagerAdapter!!.setData(list)
            mBannerPagerAdapter!!.notifyDataSetChanged()
            resetCurrentItem()
            initIndicatorDots(list)
            startTimer()
        }
    }


    private fun dpToPx(dip: Int): Int {
        return (0.5f + dip * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun resetCurrentItem() {
        if (listSize > 1 && isLooper) {
            lastPosition = Int.MAX_VALUE / 2 - ((Int.MAX_VALUE / 2) % listSize)
            mViewPager.setCurrentItem(lastPosition, false)
        } else {
            mViewPager.setCurrentItem(0, false)
        }
    }
}


