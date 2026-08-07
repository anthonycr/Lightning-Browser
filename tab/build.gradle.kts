plugins {
    id("com.android.library")
    id("com.squareup.sort-dependencies") version "0.20.0"
    id("com.autonomousapps.dependency-analysis") version "3.18.0"
}

android {
    namespace = "acr.browser.lightning.tab"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 28
    }
    lint {
        abortOnError = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {
    val kotlin = "2.4.10"
    val coroutines = "1.11.0"

    api("androidx.activity:activity:1.13.0")
    api("androidx.compose.ui:ui-graphics:1.11.4")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutines}")

    implementation(platform("androidx.compose:compose-bom:2026.06.01"))
    implementation("androidx.annotation:annotation:1.10.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${kotlin}")
}

kotlin {
    jvmToolchain(21)
}
