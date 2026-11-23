rootProject.name = "flutter_antplus"

pluginManagement {
    repositories {
        google()      // Required for Android plugins
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.android.library") version "9.0.1" apply false // Match your required AGP version
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
}