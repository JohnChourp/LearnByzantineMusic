plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinCompose)
    alias(libs.plugins.androidLegacyKapt)
}

// Minimum patched versions for vulnerable transitive dependencies. AGP 9 resolves its lint and
// UTP tool classpaths in :app configurations, which the root buildscript forces do not reach, so
// these are floors: they raise an older requested version but never lower a newer one (lint
// needs Guava 33.x, which a plain force would downgrade).
val securityFloors = mapOf(
    "com.google.guava:guava" to "32.1.3-jre",
    "org.bouncycastle:bcprov-jdk18on" to "1.85",
    "org.bouncycastle:bcpkix-jdk18on" to "1.85",
    "org.bouncycastle:bcutil-jdk18on" to "1.85",
    "org.apache.commons:commons-lang3" to "3.20.0",
    "org.apache.httpcomponents:httpclient" to "4.5.14",
)

fun isOlderThan(version: String, floor: String): Boolean {
    val parts = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val floorParts = floor.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(parts.size, floorParts.size)) {
        val a = parts.getOrElse(i) { 0 }
        val b = floorParts.getOrElse(i) { 0 }
        if (a != b) return a < b
    }
    return false
}

configurations.configureEach {
    resolutionStrategy.eachDependency {
        val floor = securityFloors["${requested.group}:${requested.name}"]
        val version = requested.version
        if (floor != null && version != null && isOlderThan(version, floor)) {
            useVersion(floor)
            because("Security floor for a vulnerable transitive version")
        }
        // Force patched Netty 4.1.x due AGP/UTP test-platform transitive dependency vulnerabilities (via grpc-netty).
        if (requested.group == "io.netty" && requested.version?.startsWith("4.1.") == true) {
            useVersion("4.1.138.Final")
            because("AGP/UTP test-platform Netty 4.1.x vulnerabilities")
        }
    }
}

android {
    namespace = "com.johnchourp.learnbyzantinemusic"
    compileSdk = 37
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
        versionCode = 55
        versionName = "1.17.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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

kapt {
    arguments {
        // Room writes every database's schema here, one JSON per version. The files are committed:
        // they are what each migration is written against (DatabaseSchemaGuardTest pins them).
        arg("room.schemaLocation", "$projectDir/schemas")
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
