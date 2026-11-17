import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Sur GitHub Actions, le numéro de version suit le numéro de fabrication :
// chaque nouvel APK s'installe ainsi par-dessus le précédent.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "io.github.cyrille31.radiop"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.cyrille31.radiop"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // Clé de signature fixe, versionnée dans le dépôt : sans elle, chaque APK fabriqué
    // par GitHub Actions aurait une signature différente et refuserait de se mettre à jour.
    // Suffisant pour un usage personnel ; à remplacer par une clé privée avant toute publication.
    signingConfigs {
        create("perso") {
            storeFile = rootProject.file("keystore/radiop.keystore")
            storePassword = "radiop"
            keyAlias = "radiop"
            keyPassword = "radiop"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("perso")
        }
        debug {
            signingConfig = signingConfigs.getByName("perso")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.browser:browser:1.9.0")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
}
