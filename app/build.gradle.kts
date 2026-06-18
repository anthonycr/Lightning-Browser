plugins {
    id("com.android.application")
    id("com.github.ben-manes.versions")
    id("com.google.devtools.ksp") version "2.3.9"
    id("com.anthonycr.plugins.mezzanine") version "2.4.0"
    id("com.autonomousapps.dependency-analysis") version "3.15.0"
    id("com.squareup.sort-dependencies") version "0.18.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0"
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 36
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

    productFlavors {
        create("lightningPlus") {
            dimension = "capabilities"
            buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"true\")")
            applicationId = "acr.browser.lightning"
            versionCode = 101
        }

        if (!isCi) {
            create("lightningLite") {
                dimension = "capabilities"
                buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"false\")")
                applicationId = "acr.browser.barebones"
                versionCode = 102
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
    val mezzanineVersion = "2.4.0"
    val daggerVersion = "2.59.2"
    val kotlin = "2.4.0"
    val datastore = "1.2.1"
    val coil = "3.5.0"
    val coroutines = "1.11.0"
    val lifecycle = "2.10.0"

    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
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
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-text")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-unit")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("androidx.core:core:1.18.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.datastore:datastore:$datastore")
    implementation("androidx.datastore:datastore-core:$datastore")
    implementation("androidx.datastore:datastore-preferences:$datastore")
    implementation("androidx.datastore:datastore-preferences-core:$datastore")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("androidx.lifecycle:lifecycle-common:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.webkit:webkit:1.16.0")
    implementation("com.anthonycr.mezzanine:core:$mezzanineVersion")
    implementation("com.google.android.material:material:1.14.0")
    implementation("com.google.dagger:dagger:$daggerVersion")
    implementation("com.guolindev.permissionx:permissionx:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.squareup.okio:okio:3.17.0")
    implementation("io.coil-kt.coil3:coil:$coil")
    implementation("io.coil-kt.coil3:coil-core:$coil")
    implementation("io.coil-kt.coil3:coil-network-okhttp:$coil")
    implementation("javax.inject:javax.inject:1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("org.jspecify:jspecify:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    compileOnly("javax.annotation:jsr250-api:1.0")

    testImplementation("com.nhaarman:mockito-kotlin:1.6.0") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines")
    testImplementation("org.mockito:mockito-core:5.23.0")
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

kotlin {
    jvmToolchain(21)
}
