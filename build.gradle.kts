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
    id("com.android.application") version "9.2.1" apply false
    id("com.github.ben-manes.versions") version "0.54.0" apply false
}
