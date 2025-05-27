plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.monster.literaryflow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.monster.literaryflow"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
        dataBinding = true
    }
    kapt {
        correctErrorTypes = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":fast"))
    implementation(fileTree("libs").include("*.jar", "*.aar"))
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation(platform("androidx.compose:compose-bom:2023.03.00"))
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation ("androidx.constraintlayout:constraintlayout-compose:1.0.1")
    //glide
    implementation("com.github.bumptech.glide:glide:4.12.0")
    //Logger
//    implementation("com.orhanobut:logger:2.2.0")

    implementation ("com.github.chrisbanes:PhotoView:2.3.0")

    val epoxyVersion = "4.6.3"
    implementation("com.airbnb.android:epoxy:$epoxyVersion")
    kapt("com.airbnb.android:epoxy-processor:$epoxyVersion")

    implementation ("com.github.getActivity:XXPermissions:18.5")
    //coil图片加载
    implementation ("io.coil-kt:coil-compose:1.4.0")

    //room数据库
    implementation ("androidx.room:room-runtime:2.4.2") {
        exclude(group = "com.intellij", module = "annotations")
    }
    kapt ("androidx.room:room-compiler:2.4.2") {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation ("androidx.room:room-ktx:2.4.2") {
        exclude(group = "com.intellij", module = "annotations")
    }


    implementation ("com.google.code.gson:gson:2.8.6")

    //nanoHttpd 本地服务器
    implementation ("org.nanohttpd:nanohttpd:2.3.1")

    //阴影效果
    implementation ("com.google.android.material:material:1.9.0")

    //BRV recycleView Adapter  https://github.com/liangjingkanji/BRV
    implementation ("com.github.liangjingkanji:BRV:1.6.1")

    //MlKit文字识别
    implementation ("com.google.mlkit:text-recognition:16.0.1")

    implementation ("com.google.mlkit:text-recognition-chinese:16.0.1")

    //lottie动画
    implementation ("com.airbnb.android:lottie:6.4.0")
    implementation ("com.airbnb.android:lottie-compose:6.4.0")


//    implementation ("com.google.accompanist:accompanist-drawablepainter:0.32.0")

}
tasks.withType(type = org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask::class) {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}
configurations.all {
    resolutionStrategy {
        force("com.intellij:annotations:12.0")
    }
}
