import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.jetbrainsKotlinCompose)
    id("org.jetbrains.kotlin.kapt")
}

configurations.configureEach {
    resolutionStrategy.force("com.google.guava:guava:32.1.3-jre")
    // Force patched Netty 4.1.x due AGP/UTP test-platform transitive dependency vulnerabilities (via grpc-netty).
    resolutionStrategy.eachDependency {
        if (requested.group == "io.netty" && requested.version?.startsWith("4.1.") == true) {
            useVersion("4.1.138.Final")
            because("AGP/UTP test-platform Netty 4.1.x vulnerabilities")
        }
    }
}

android {
    namespace = "com.johnchourp.learnbyzantinemusic"
    compileSdk = 36
    val signingStoreFile = System.getenv("ANDROID_SIGNING_STORE_FILE")
    val signingStorePassword = System.getenv("ANDROID_SIGNING_STORE_PASSWORD")
    val signingKeyAlias = System.getenv("ANDROID_SIGNING_KEY_ALIAS")
    val signingKeyPassword = System.getenv("ANDROID_SIGNING_KEY_PASSWORD")
    val hasReleaseSigning =
        !signingStoreFile.isNullOrBlank() &&
            !signingStorePassword.isNullOrBlank() &&
            !signingKeyAlias.isNullOrBlank() &&
            !signingKeyPassword.isNullOrBlank()

    defaultConfig {
        applicationId = "com.johnchourp.learnbyzantinemusic"
        minSdk = 24
        targetSdk = 34
        versionCode = 53
        versionName = "1.15.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

dependencies {

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    implementation(libs.guava)
    kapt(libs.androidx.room.compiler)
    // Room's processor reads Kotlin metadata with kotlin-metadata-jvm 2.2.0, which stops at metadata 2.3;
    // keep the reader on the compiler's Kotlin version so kapt can read what Kotlin 2.4+ writes.
    kapt(libs.kotlin.metadata.jvm)
    kapt(libs.guava)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.work.runtime)
    implementation("com.arthenica:ffmpeg-kit-full-gpl:6.0-2")
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
