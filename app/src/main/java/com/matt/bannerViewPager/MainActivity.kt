package com.matt.bannerViewPager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.matt.bannerViewPager.banner.BannerViewPager
import com.matt.bannerViewPager.banner.GalleryTransformer

/**
 * @ Author : 廖健鹏
 * @ Date : 2021/7/27
 * @ e-mail : 329524627@qq.com
 * @ Description :
 */
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
            setInterval(2)
            //设置显示多个视图的宽度
            setRevealWidth(50)
            //设置视图间隔
            setPageMargin(8)
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
    }
}