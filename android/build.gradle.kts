group = "me.heyteacher.flutter_antplus"
version = "1.0"

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.11.2")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.android.library")
}

android {
    namespace = "me.heyteacher.flutter_antplus"

    compileSdk = 33

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests.all {
            it.outputs.upToDateWhen { false }

            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
                showStandardStreams = true
            }
        }
    }
}

dependencies {
    compileOnly(files("libs/flutter.jar"))
    implementation(fileTree("libs/android-antplus-plugin-lib-release_3.9.0/"))
    implementation(files("libs/fit_21.105.00.jar"))
    implementation("androidx.annotation:annotation:1.10.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.0.0")
    testCompileOnly(files("libs/flutter.jar"))
}
