buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "9.3.0" apply false
    id("com.github.ben-manes.versions") version "0.54.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
}
