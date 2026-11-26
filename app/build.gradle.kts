plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "dev.joyal.lodestill"
    compileSdk = 34
    defaultConfig {
        applicationId = "dev.joyal.lodestill"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    buildFeatures { compose = true }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// connectedDebugAndroidTest reinstalls the app at suite start, and the
// reinstall force-stops the package, which strips our accessibility service
// from enabled_accessibility_services before any test runs. Re-enable it
// after the install tasks and before the first test (i.e. before UiAutomation
// is registered), matching the pre-suite path that provably binds.
val enableLodestillA11yForTests = tasks.register("enableLodestillA11yForTests") {
    mustRunAfter("installDebug", "installDebugAndroidTest")
    doLast {
        val adb = File(System.getenv("ANDROID_HOME") ?: "", "platform-tools/adb").absolutePath
        exec { commandLine(adb, "shell", "settings", "put", "secure",
            "enabled_accessibility_services",
            "dev.joyal.lodestill.debug/dev.joyal.lodestill.service.BlockAccessibilityService") }
        exec { commandLine(adb, "shell", "settings", "put", "secure", "accessibility_enabled", "1") }
    }
}
tasks.named("connectedDebugAndroidTest") { dependsOn(enableLodestillA11yForTests) }
