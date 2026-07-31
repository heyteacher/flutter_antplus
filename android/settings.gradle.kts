rootProject.name = "flutter_antplus"

pluginManagement {
    repositories {
        google()      // Required for Android plugins
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.android.library") version "8.3.2" apply false // Match your required AGP version
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}