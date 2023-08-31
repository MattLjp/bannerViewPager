package com.matt.myapplication

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.matt.bannerviewpager.transformer.ScaleTransformer
import com.matt.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        val adapter = BannerAdapter()
        binding.bannerViewpager
            .setAutoPlay(true)
            .setCanLoop(true)
            .setInterval(3000)
            .seScrollDuration(1000)
            .setPageMargin(dpToPx(15))
            .setRevealWidth(dpToPx(25))
            .addPageTransformer(ScaleTransformer())
            .setIndicatorSliderColor(
                R.drawable.dot_def,
                R.drawable.dot_selected
            )
            .setCanShowIndicator(true)
            .setAdapter(adapter)
            .create(
                listOf(
                    R.mipmap.b,
                    R.mipmap.c,
                    R.mipmap.d,
                    R.mipmap.e,
                    R.mipmap.f
                )
            )
    }



    private fun dpToPx(dip: Int): Int {
        return (0.5f + dip * Resources.getSystem().displayMetrics.density).toInt()
    }
}