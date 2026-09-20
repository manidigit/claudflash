plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.claudemani.database"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// KSP's own DSL extension is a top-level Gradle extension registered by
// the `com.google.devtools.ksp` plugin — it is NOT a member of the
// Android Gradle Plugin's `LibraryExtension` (the `android { }` block
// above). It must sit as a sibling of `android { }` and `dependencies { }`.
// Bug found and fixed 2026-09-18: this was previously nested *inside*
// `android { }`, which fails at Gradle script compilation with
// "Unresolved reference: ksp" and blocks Configuration for the entire
// build (every other module depends on :database). Caught by an external
// build audit — none of this project's own tests could ever have caught
// it, since nothing in this sandboxed environment actually runs Gradle.
ksp {
    // Room schema export location (kept out of VCS via .gitignore; used
    // only for Room's internal schema-diffing during Migration work).
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(project(":domain"))

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("javax.inject:javax.inject:1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.room:room-testing:2.6.1")

    // Real-Room integration tests run as JVM unit tests via Robolectric
    // instead of true instrumented androidTest (which would need an
    // emulator in CI) — see the KDoc on LanguagePairActiveIndexTest for
    // the full reasoning (Phase 29 decision).
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
}
