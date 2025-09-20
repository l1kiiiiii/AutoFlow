

    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.kotlin.compose)
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
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            }
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_18
            targetCompatibility = JavaVersion.VERSION_18
        }
        kotlin {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_18)
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
    }