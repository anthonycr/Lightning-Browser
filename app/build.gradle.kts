import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("jacoco")
    id("com.github.ben-manes.versions")
    id("com.google.devtools.ksp") version "2.1.0-1.0.29"
    id("com.anthonycr.mezzanine.plugin") version "2.0.0"
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        targetSdk = 35
        versionName = "5.1.0"
        vectorDrawables.useSupportLibrary = true
    }

    sourceSets {
        create("lightningPlus").apply {
            setRoot("src/LightningPlus")
        }
        create("lightningLite").apply {
            setRoot("src/LightningLite")
        }
    }

    buildFeatures {
        viewBinding = true
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
            isMinifyEnabled = true
            isShrinkResources = true
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

    productFlavors {
        create("lightningPlus") {
            dimension = "capabilities"
            buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"true\")")
            applicationId = "acr.browser.lightning"
            versionCode = 101
        }

        create("lightningLite") {
            dimension = "capabilities"
            buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"false\")")
            applicationId = "acr.browser.barebones"
            versionCode = 102
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

jacoco {
    toolVersion = "0.8.8" // See http://www.eclemma.org/jacoco/
}

dependencies {
    // multidex debug
    debugImplementation("androidx.multidex:multidex:2.0.1")

    // test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:3.11.2")
    testImplementation("com.nhaarman:mockito-kotlin:1.6.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("org.robolectric:robolectric:4.4")

    // support libraries
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.webkit:webkit:1.12.1")

    // html parsing for reading mode
    implementation("org.jsoup:jsoup:1.15.3")

    // file reading
    val mezzanineVersion = "2.0.0"
    implementation("com.anthonycr.mezzanine:core:$mezzanineVersion")
    ksp("com.anthonycr.mezzanine:processor:$mezzanineVersion")

    // dependency injection
    val daggerVersion = "2.55"
    implementation("com.google.dagger:dagger:$daggerVersion")
    kapt("com.google.dagger:dagger-compiler:$daggerVersion")
    compileOnly("javax.annotation:jsr250-api:1.0")

    // permissions
    implementation("com.guolindev.permissionx:permissionx:1.7.1")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // rx
    implementation("io.reactivex.rxjava3:rxjava:3.1.6")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")

    // memory leak analysis
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    // kotlin
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
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

kapt {
    correctErrorTypes = true
    useBuildCache = true
    generateStubs = true
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_1)
    }
}

java {
    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
}
