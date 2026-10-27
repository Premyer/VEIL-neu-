plugins {
    id("com.android.application")
}
android {
    namespace = "de.veil.browser"
    compileSdk = 37
    defaultConfig {
        applicationId = "de.veil.browser"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
dependencies {
    implementation("androidx.core:core:1.19.1")
    testImplementation("junit:junit:4.13.2")
    implementation("org.mozilla.geckoview:geckoview:159.0.20261022150525")
}
