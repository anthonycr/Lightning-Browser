buildscript {
    dependencies {
        // Force metadata to match Kotlin version
        // See https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1661
        classpath("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.10")
    }
}

plugins {
    id("com.android.application") version "9.3.1" apply false
    id("com.github.ben-manes.versions") version "0.56.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.10" apply false
}
