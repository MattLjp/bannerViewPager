## 前言

在开发中经常会遇到循环轮播图，之前的实现方式是在Activity中使用定时器控制轮播。后面想了想，看能不能把ViewPager2和定时器封装成自定义控件方便移植。然后就有了这个自定义控件。

## 效果

![在这里插入图片描述](doc/img/20230831164415.gif)
![在这里插入图片描述](doc/img/20230831164831.gif)
![在这里插入图片描述](doc/img/20230831163354.gif)

## 如何使用

###  gradle依赖
Project 的 settings.gradle 添加仓库
```groovy
dependencyResolutionManagement {
    repositories {
        // ...
        maven { url 'https://jitpack.io' }
    }
}
```

Module 的 build.gradle 添加依赖框架
```groovy
dependencies {
    implementation 'com.github.MattLjp:bannerViewPager:1.0.3'
}
```

### 基础用法
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

    override fun getLayoutId(viewType: Int) = R.layout.item_banner

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

item_banner.xml

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

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        val adapter = BannerAdapter()
        binding.bannerViewpager
            .setAutoPlay(true)  //设置自动轮播
            .setCanLoop(true) //设置是否循环
            .setInterval(3000) //设置轮播间隔时间
            .seScrollDuration(1000) //设置滚动时长
            .setPageMargin(dpToPx(15)) //设置页边距
//            .setRevealWidth(dpToPx(35)) //一屏多页模式下两边页面显露出来的宽度
//            .addPageTransformer(ScaleTransformer()) //切换效果
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
```
在创建完成后，可以通过`refreshData()`方法，更新列表

切换效果可以继承`ViewPager2.PageTransformer`自己实现，当前预设了几种效果

**ScaleTransformer**

![在这里插入图片描述](doc/img/20230831164831.gif)

**AlphaPageTransformer**

![在这里插入图片描述](doc/img/20230831163354.gif)

**DepthPageTransformer**

![在这里插入图片描述](doc/img/20230831163909.gif)

**RotateDownPageTransformer**

![在这里插入图片描述](doc/img/20230831164944.gif)

**RotateUpPageTransformer**

![在这里插入图片描述](doc/img/20230831165156.gif)

**RotateYTransformer**

![在这里插入图片描述](doc/img/20230831165449.gif)

**ZoomOutPageTransformer**

![在这里插入图片描述](doc/img/20230831165621.gif)


## License
```
 Copyright 2018, jessyan

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
