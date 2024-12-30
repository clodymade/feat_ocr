plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    `maven-publish` // Maven publishing for JitPack
}

android {
    namespace = "com.mwkg.feat_ocr"
    compileSdk = 35

    defaultConfig {
        minSdk = 31

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        buildConfig = true
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // AndroidX Core 및 Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom.v20240100))

    // Compose UI, Material
    implementation(libs.androidx.ui) // Compose UI
    implementation(libs.androidx.material3) // Material 3
    implementation(libs.androidx.lifecycle.viewmodel.compose) // ViewModel Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.play.services.mlkit.text.recognition.common)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.play.services.mlkit.barcode.scanning)
    implementation(libs.androidx.window) // Activity Compose

    debugImplementation(libs.androidx.compose.ui.ui.tooling)

    // Core KTX
    implementation(libs.androidx.core.ktx) // Core KTX v1.15.0

    testImplementation(libs.junit) // JUnit
    androidTestImplementation(libs.androidx.junit) // AndroidX JUnit
    androidTestImplementation(libs.androidx.espresso.core) // Espresso UI Test

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit, CameraX
    implementation(libs.androidx.camera.mlkit.vision)
    implementation(libs.text.recognition) // ML Kit Text Recognition

    // OpenCV
    implementation(libs.opencv)
}

// Maven publishing configuration for JitPack
publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }

            // Define Maven artifact metadata
            groupId = "com.github.clodymade"    // GitHub username as group ID
            artifactId = "feat_ocr"             // Module name as artifact ID
            version = "1.0.0"                   // Version matching the Git tag

            // Configure POM metadata
            pom {
                name.set("feat_ocr")
                description.set("A utility library for Android apps.")
                url.set("https://github.com/clodymade/feat_ocr")

                licenses {
                    license {
                        name.set("Apache 2.0 License") // License type
                        url.set("https://opensource.org/license/apache-2-0")
                    }
                }

                developers {
                    developer {
                        id.set("clodymade")
                        name.set("netcanis")
                        email.set("netcanis@gmail.com")
                    }
                }

                scm {
                    // Source Control Management (SCM) details
                    connection.set("scm:git:git://github.com/clodymade/feat_ocr.git")
                    developerConnection.set("scm:git:ssh://git@github.com:clodymade/feat_ocr.git")
                    url.set("https://github.com/clodymade/feat_ocr")
                }
            }
        }
    }
}
