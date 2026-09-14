plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

fun getGitCommitCount(): Int {
    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .directory(rootDir)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output.toIntOrNull() ?: 100
    } catch (_: Throwable) {
        100
    }
}

val baseVersion = "2.0"
val buildNumber: Int = getGitCommitCount()

android {
    namespace = "com.sloosh.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sloosh.tv"
        minSdk = 26
        targetSdk = 34
        versionCode = buildNumber
        versionName = "$baseVersion.$buildNumber"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keys/slooshkey")
            val localProps = java.util.Properties().apply {
                val propFile = rootProject.file("local.properties")
                if (propFile.exists()) {
                    propFile.inputStream().use { load(it) }
                }
            }
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: localProps.getProperty("KEYSTORE_PASSWORD")
                ?: (project.findProperty("KEYSTORE_PASSWORD") as? String)

            if (keystoreFile.exists() && !keystorePassword.isNullOrBlank()) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: localProps.getProperty("KEY_ALIAS")
                    ?: (project.findProperty("KEY_ALIAS") as? String)
                    ?: "keysloosh"
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: localProps.getProperty("KEY_PASSWORD")
                    ?: (project.findProperty("KEY_PASSWORD") as? String)
                    ?: keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                println(">>> BUILD: Signing release APK with: ${releaseSigning.storeFile?.name}")
                signingConfig = releaseSigning
            } else {
                println(">>> BUILD: Release keystore or password not found, falling back to debug signing")
                signingConfig = signingConfigs.getByName("debug")
            }
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
        freeCompilerArgs += listOf(
            "-opt-in=androidx.tv.material3.ExperimentalTvMaterial3Api",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INDEX/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM & TV
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.navigation.compose)

    // ExoPlayer Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coil Image Loading
    implementation(libs.coil.compose)

    // Palette Color Extraction
    implementation(libs.androidx.palette)

    // AndroidX WebKit for document-start script injection & cross-origin iframe hooking
    implementation(libs.androidx.webkit)

    debugImplementation(libs.androidx.ui.tooling)
}
