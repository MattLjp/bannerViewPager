# 一、前言
在开发中经常会遇到循环轮播图，之前的实现方式是在Activity中使用定时器控制轮播。后面想了想，看能不能把ViewPager2和定时器封装成自定义控件方便移植。然后就有了这个自定义控件。

# 二、控件实现
因为使用的是ViewPager2，所以要用到适配器，这里写了个`BaseBannerAdapter`用来设置循环滚动

```kotlin
/**
 * @ Author : 廖健鹏
 * @ Date : 2021/7/27
 * @ e-mail : 329524627@qq.com
 * @ Description :[BannerViewPager] 轮播控件所需的 BaseAdapter
 */
abstract class BaseBannerAdapter<T, VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    protected var mList: MutableList<T> = mutableListOf()
    var isCanLoop = false
    var pageClickListener: OnPageClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflate =
            LayoutInflater.from(parent.context).inflate(getLayoutId(viewType), parent, false)
        return createViewHolder(parent, inflate, viewType)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val realPosition: Int = getRealPosition(position)
        holder.itemView.setOnClickListener {
            pageClickListener?.onPageClick(realPosition)
        }
        onBind(holder, mList[realPosition], realPosition, mList.size)
    }

    override fun getItemCount(): Int {
        return if (isCanLoop && mList.size > 1) {
            Int.MAX_VALUE
        } else {
            mList.size
        }
    }

    fun getData(): List<T> {
        return mList
    }

    fun setData(list: List<T>) {
        mList.clear()
        mList.addAll(list)
    }

    fun getListSize(): Int {
        return mList.size
    }

    fun getRealPosition(position: Int): Int {
        val pageSize = mList.size
        if (pageSize == 0) {
            return 0
        }
        return if (isCanLoop) (position + pageSize) % pageSize else position
    }

    interface OnPageClickListener {
        fun onPageClick(position: Int)
    }


    protected abstract fun onBind(holder: VH, data: T, position: Int, pageSize: Int)
    abstract fun createViewHolder(parent: ViewGroup, itemView: View, viewType: Int): VH
    abstract fun getLayoutId(viewType: Int): Int

}

```

**BannerViewPager.kt**

```kotlin
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

```
**bvp_layout.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clipChildren="false"
    android:layerType="software">

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/vp_main"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipChildren="false" />

    <LinearLayout
        android:id="@+id/bvp_layout_indicator"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:gravity="center"
        android:orientation="horizontal" />
</RelativeLayout>
```
还有两个指示器图片**shape_dot.xml**、**shape_dot_selected.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size
        android:width="8dp"
        android:height="8dp"/>
    <corners
        android:radius="8dp"/>
    <solid
        android:color="#ffffff"/>
</shape>

```

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <size
        android:width="8dp"
        android:height="8dp"/>
    <corners
        android:radius="8dp"/>
    <solid
        android:color="#00ccff"/>
</shape>

```
# 三、使用控件
使用起来很简单
**1. 在xml中添加控件**
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <com.example.myapplication.banner.BannerViewPager
        android:id="@+id/viewpager2"
        android:layout_width="match_parent"
        android:layout_height="400dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />
</androidx.constraintlayout.widget.ConstraintLayout>
```
**2. 继承BaseBannerAdapter实现适配器**

```kotlin
class BannerAdapter : BaseBannerAdapter<Int, BannerAdapter.ViewHolder>() {

    override fun getLayoutId(viewType: Int) = R.layout.item_banner_samll

    override fun onBind(holder: ViewHolder, data: Int, position: Int, pageSize: Int) {

        holder.imageView.setImageResource(data)
    }


    override fun createViewHolder(parent: ViewGroup, itemView: View, viewType: Int): ViewHolder {
        return ViewHolder(itemView)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.iv_banner)
    }
}
```
item_banner_samll.xml
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical">

    <ImageView
        android:id="@+id/iv_banner"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitXY" />
</LinearLayout>
```

**3. 在Activity中配置和使用**

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var viewPager2: BannerViewPager<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewPager2 = findViewById(R.id.viewpager2)

        val adapter = BannerAdapter()
        viewPager2.apply {
            //设置生命周期
            setLifecycleRegistry(lifecycle)
            //开启自动轮询
            setAutoPlay(true)
            //开启循环滚动
            setCanLoop(true)
            //设置轮询间隔
            setInterval(3)
            //显示指示器
            setCanShowIndicator(true)
            //设置适配器
            setAdapter(adapter)
        }.create(
            listOf(
                R.mipmap.b,
                R.mipmap.c,
                R.mipmap.d,
                R.mipmap.e,
                R.mipmap.f
            )
        )
    }
}
```

效果如下
![在这里插入图片描述](https://img-blog.csdnimg.cn/5cf641d5036d4d348f8a649e93cc3c21.gif)
除了上面的配置，还可以配置指示器的图片、间隔，图片间的间隔，展示多个视图，如下

```kotlin
        viewPager2.apply {
            //设置生命周期
            setLifecycleRegistry(lifecycle)
            //开启自动轮询
            setAutoPlay(true)
            //开启循环滚动
            setCanLoop(true)
            //设置轮询间隔
            setInterval(2)
            //设置显示多个视图的宽度
            setRevealWidth(50)
            //设置视图间隔
            setPageMargin(8)
            //显示指示器
            setCanShowIndicator(true)
            //设置适配器
            setAdapter(adapter)
        }.create(
            listOf(
                R.mipmap.b,
                R.mipmap.c,
                R.mipmap.d,
                R.mipmap.e,
                R.mipmap.f
            )
        )
```

效果如下
![在这里插入图片描述](https://img-blog.csdnimg.cn/aa230c1637dc46f1ae49daaa33bd3280.gif)
如果要添加滚动动画可以继承ViewPager2.PageTransformer实现

```kotlin
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
```

```kotlin
        viewPager2.apply {
            //设置生命周期
            setLifecycleRegistry(lifecycle)
            //开启自动轮询
            setAutoPlay(true)
            //开启循环滚动
            setCanLoop(true)
            //设置轮询间隔
            setInterval(2)
            //设置显示多个视图的宽度
            setRevealWidth(50)
            //设置滚动动画
            setPageTransformer(GalleryTransformer())
            //显示指示器
            setCanShowIndicator(true)
            //设置适配器
            setAdapter(adapter)
        }.create(
            listOf(
                R.mipmap.b,
                R.mipmap.c,
                R.mipmap.d,
                R.mipmap.e,
                R.mipmap.f
            )
        )
```

效果如下
![在这里插入图片描述](https://img-blog.csdnimg.cn/def7c990dd784c73983202d69f2c5df1.gif)

目前功能只有这些，后续遇到新需求再慢慢完善。
