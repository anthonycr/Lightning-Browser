import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("jacoco")
    id("com.github.ben-manes.versions")
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33
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
            isTestCoverageEnabled = true
        }

        named("release") {
            multiDexEnabled = false
            isMinifyEnabled = true
            isShrinkResources = true
            setProguardFiles(listOf("proguard-project.txt"))
            isTestCoverageEnabled = false

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
    packagingOptions {
        resources {
            excludes += listOf(".readme")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
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
    implementation("androidx.annotation:annotation:1.5.0")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.1.0")
    implementation("androidx.appcompat:appcompat:1.6.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.core:core-ktx:1.10.0-alpha02")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("androidx.drawerlayout:drawerlayout:1.1.1")
    implementation("androidx.preference:preference:1.2.0")

    // html parsing for reading mode
    implementation("org.jsoup:jsoup:1.15.3")

    // file reading
    val mezzanineVersion = "1.1.1"
    implementation("com.anthonycr.mezzanine:mezzanine:$mezzanineVersion")
    kapt("com.anthonycr.mezzanine:mezzanine-compiler:$mezzanineVersion")

    // dependency injection
    val daggerVersion = "2.44.2"
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

    // tor proxy
    val netCipherVersion = "2.0.0-alpha1"
    implementation("info.guardianproject.netcipher:netcipher:$netCipherVersion")
    implementation("info.guardianproject.netcipher:netcipher-webkit:$netCipherVersion")

    implementation("com.anthonycr.progress:animated-progress:1.0")

    // memory leak analysis
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.10")

    // kotlin
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
}

kapt {
    arguments {
        arg("mezzanine.projectPath", project.rootDir)
    }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "1.8"
        kotlinOptions {
            freeCompilerArgs += listOf("-XXLanguage:+InlineClasses")
            freeCompilerArgs += listOf("-progressive")
        }
    }
}
