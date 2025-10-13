
    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.kotlin.compose)
        id("kotlin-kapt")
    }

    android {
        namespace = "com.example.autoflow"
        compileSdk = 36

        defaultConfig {
            applicationId = "com.example.autoflow"
            minSdk = 31
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        buildTypes {
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                signingConfig = signingConfigs.getByName("debug")
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }
        kotlin {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            }
        }
        buildFeatures {
            compose = true
        }
    }

    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlinx-metadata-jvm:1.0.0-RC3")
    }

    dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)

        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material)
        implementation(libs.androidx.compose.material3)

        implementation(libs.androidx.room.common.jvm)
        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.compose.ui.text)
        annotationProcessor(libs.androidx.room.compiler) // Using libs.androidx.room.compiler

        implementation(libs.androidx.work.runtime)
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.androidx.lifecycle.livedata)

        implementation(libs.google.play.services.location)

        testImplementation(libs.junit)
        testImplementation(libs.mockito.core)

        androidTestImplementation(libs.androidx.test.runner)
        androidTestImplementation(libs.androidx.test.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)

        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.test.manifest)

        implementation(libs.androidx.compose.material.icons.extended)
        implementation(libs.androidx.work.runtime)

        implementation(libs.androidx.navigation.compose)

        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.compose.runtime.livedata)

        // Google Play Services Location
        implementation(libs.google.play.services.location)
        implementation(libs.play.services.maps)

        implementation(libs.kotlinx.coroutines.play.services)

        implementation(libs.rhino.android)

        // For HTTP requests and network operations
        implementation(libs.okhttp)

        // Google Maps Compose
        implementation(libs.maps.compose)
        implementation(libs.maps.compose.utils)
        implementation(libs.maps.compose.widgets)

        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
        kapt(libs.androidx.room.compiler)

        implementation(libs.androidx.work.runtime.ktx)

    }