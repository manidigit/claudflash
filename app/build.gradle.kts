plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.flashlearn.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.flashlearn.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "1.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":database"))
    implementation(project(":data"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Theme.FlashLearn's parent (Theme.Material3.DayNight.NoActionBar, in
    // themes.xml) is an XML style published by the CLASSIC View-system
    // Material Components library, not by androidx.compose.material3
    // (Compose-only, publishes zero XML resources). Without this,
    // AAPT2 resource-linking fails with "style not found" and the app
    // module never builds — even though every Compose material3 API call
    // in the codebase itself compiles fine. This dependency exists purely
    // so the Activity has a valid pre-Compose window theme; found missing
    // by an external build audit 2026-09-18.
    implementation("com.google.android.material:material:1.11.0")

    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    // androidTestImplementation does NOT extend implementation (unlike
    // debugImplementation, which does) — a platform()/BOM import only
    // constrains configurations that either extend the one it was
    // declared on or import it themselves. Without this line,
    // ui-test-junit4 below has no version and fails dependency
    // resolution. Found missing by an external build audit 2026-09-18.
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
