buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.13.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    }
}

plugins {
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}
