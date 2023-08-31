package com.matt.myapplication

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.matt.bannerviewpager.transformer.AlphaPageTransformer
import com.matt.bannerviewpager.transformer.DepthPageTransformer
import com.matt.bannerviewpager.transformer.MZScaleInTransformer
import com.matt.bannerviewpager.transformer.RotateDownPageTransformer
import com.matt.bannerviewpager.transformer.RotateUpPageTransformer
import com.matt.bannerviewpager.transformer.RotateYTransformer
import com.matt.bannerviewpager.transformer.ScaleTransformer
import com.matt.bannerviewpager.transformer.ZoomOutPageTransformer
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
            .setRevealWidth(dpToPx(35))
            .addPageTransformer(ZoomOutPageTransformer())
            .setIndicatorSliderColor(
                R.drawable.dot_def,
                R.drawable.dot_selected
            )
            .setCanShowIndicator(true)
            .setAdapter(adapter)
            .create(
                listOf(
                    R.drawable.a,
                    R.drawable.b,
                    R.drawable.c,
                    R.drawable.d,
                    R.drawable.e,
                    R.drawable.f,
                )
            )
    }



    private fun dpToPx(dip: Int): Int {
        return (0.5f + dip * Resources.getSystem().displayMetrics.density).toInt()
    }
}