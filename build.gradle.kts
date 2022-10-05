buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.2.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.36.0")
    }

    extra.apply{
        set("kotlinVersion", "1.4.21")
        set("minSdkVersion", 21)
        set("targetSdkVersion", 30)
        set("buildToolsVersion", 30)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}
