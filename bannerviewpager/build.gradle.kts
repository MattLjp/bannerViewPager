plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.matt.bannerviewpager"
    compileSdk = 33

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

val GROUP_ID = "com.github.MattLjp"
val ARTIFACT_ID = "bannerViewPager"
val VERSION = latestGitTag().ifEmpty { "1.0.0" }

fun latestGitTag(): String {
    val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0").start()
    return process.inputStream.bufferedReader().use { bufferedReader ->
        bufferedReader.readText().trim()
    }
}

publishing { // 发布配置
    publications { // 发布的内容
        register<MavenPublication>("release") { // 注册一个名字为 release 的发布内容
            groupId = GROUP_ID
            artifactId = ARTIFACT_ID
            version = VERSION
        }
    }
}


dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}