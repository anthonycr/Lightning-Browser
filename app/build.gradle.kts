import app.cash.licensee.SpdxId

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp") version "2.3.11"
    id("com.anthonycr.plugins.mezzanine") version "2.5.0"
    id("com.autonomousapps.dependency-analysis") version "3.18.0"
    id("com.squareup.sort-dependencies") version "0.20.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("com.anthonycr.plugins.mockingbird") version "3.3.0"
    id("app.cash.licensee") version "1.14.1"
}

android {
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        buildToolsVersion = "37.0.0"
        minSdk = 28
        targetSdk = 37
        versionName = "5.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    val isCi = System.getenv("CI") == "true"

    sourceSets {
        create("lightningPlus").apply {
            setRoot("src/LightningPlus")
        }
        if (!isCi) {
            create("lightningLite").apply {
                setRoot("src/LightningLite")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    buildTypes {
        named("debug") {
            multiDexEnabled = true
            isMinifyEnabled = false
            isShrinkResources = false
            setProguardFiles(listOf("proguard-project.txt"))
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false
        }

        named("release") {
            multiDexEnabled = false
            isMinifyEnabled = !isCi
            isShrinkResources = !isCi
            setProguardFiles(listOf("proguard-project.txt"))
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false

            ndk {
                abiFilters.add("arm64-v8a")
                abiFilters.add("armeabi-v7a")
                abiFilters.add("armeabi")
                abiFilters.add("mips")
            }
        }
    }

    flavorDimensions.add("capabilities")

    val commonVersionCode = 102

    productFlavors {
        create("lightningPlus") {
            dimension = "capabilities"
            buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"true\")")
            applicationId = "acr.browser.lightning"
            versionCode = commonVersionCode
        }

        if (!isCi) {
            create("lightningLite") {
                dimension = "capabilities"
                buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"false\")")
                applicationId = "acr.browser.barebones"
                versionCode = commonVersionCode
            }
        }
    }
    packaging {
        resources {
            excludes += listOf(".readme")
        }
    }
    lint {
        abortOnError = true
    }
    namespace = "acr.browser.lightning"
}

dependencies {
    val robolectric = "4.16.1"
    val mezzanineVersion = "2.5.0"
    val daggerVersion = "2.60.1"
    val kotlin = "2.4.10"
    val datastore = "1.2.1"
    val coil = "3.5.0"
    val coroutines = "1.11.0"
    val leakcanary = "2.14"
    val lifecycle = "2.11.0"


    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.annotation:annotation:1.10.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-geometry")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-unit")
    implementation("androidx.core:core:1.19.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.datastore:datastore:$datastore")
    implementation("androidx.datastore:datastore-core:$datastore")
    implementation("androidx.datastore:datastore-preferences:$datastore")
    implementation("androidx.datastore:datastore-preferences-core:$datastore")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.lifecycle:lifecycle-common:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.webkit:webkit:1.16.0")
    implementation("com.anthonycr.mezzanine:core:$mezzanineVersion")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.google.dagger:dagger:$daggerVersion")
    implementation("com.guolindev.permissionx:permissionx:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.squareup.okio:okio:3.18.1")
    implementation("io.coil-kt.coil3:coil-compose:$coil")
    implementation("io.coil-kt.coil3:coil-compose-core:${coil}")
    implementation("io.coil-kt.coil3:coil-core:$coil")
    implementation("io.coil-kt.coil3:coil-network-okhttp:$coil")
    implementation("javax.inject:javax.inject:1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.jsoup:jsoup:1.23.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("com.squareup.leakcanary:leakcanary-android-core:$leakcanary")
    debugImplementation("com.squareup.leakcanary:shark:$leakcanary")

    compileOnly("javax.annotation:jsr250-api:1.0")

    debugRuntimeOnly("com.squareup.leakcanary:leakcanary-android:$leakcanary")

    testImplementation("app.cash.turbine:turbine:1.2.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:${kotlin}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines")
    testImplementation("org.robolectric:annotations:$robolectric")
    testImplementation("org.robolectric:robolectric:$robolectric")

    ksp("com.anthonycr.mezzanine:processor:$mezzanineVersion")
    ksp("com.google.dagger:dagger-compiler:$daggerVersion")
}

mezzanine {
    files = files(
        "src/main/html/list.html",
        "src/main/html/bookmarks.html",
        "src/main/html/homepage.html",
        "src/main/js/InvertPage.js",
        "src/main/js/TextReflow.js",
        "src/main/js/ThemeColor.js"
    )
}

licensee {
    bundleAndroidAsset = true
    androidAssetReportPath = "licensee/artifacts.json"

    allow(SpdxId.Apache_20)
    allow(SpdxId.BSD_3_Clause)
    allowUrl("https://jsoup.org/license")
}

kotlin {
    jvmToolchain(21)
}
