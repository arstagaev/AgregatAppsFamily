import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.0"
    id("com.squareup.sqldelight") version "1.5.5" // or your current
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val ktor = "2.3.12" // keep this consistent everywhere

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation("io.ktor:ktor-client-okhttp:2.3.12")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            implementation("androidx.datastore:datastore-preferences:1.1.7") // optional if you prefer DataStore later
            implementation("com.squareup.sqldelight:android-driver:1.5.5")

            // Koin for Android
            implementation("io.insert-koin:koin-android:4.1.0")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation("com.arkivanov.decompose:decompose:3.4.0")
            implementation("com.arkivanov.decompose:extensions-compose:3.4.0")
            implementation("com.arkivanov.decompose:extensions-compose-experimental:3.4.0")

            // ktor
            implementation("io.ktor:ktor-http:$ktor")
            implementation("io.ktor:ktor-client-core:$ktor")
            // Logging plugin
            implementation("io.ktor:ktor-client-logging:$ktor")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor")
            // ktor END
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

            implementation("br.com.devsrsouza.compose.icons:feather:1.1.1")

            implementation("com.squareup.sqldelight:runtime:1.5.5")
            implementation("com.squareup.sqldelight:coroutines-extensions:1.5.5")

            // koin
            implementation("io.insert-koin:koin-core:4.1.0")
            implementation("io.insert-koin:koin-compose:4.1.0")

            // time
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

//            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation("com.russhwolf:multiplatform-settings:1.3.0")
            implementation("com.russhwolf:multiplatform-settings-serialization:1.3.0")
        }
        iosMain.dependencies {
            implementation("com.squareup.sqldelight:native-driver:1.5.5")
            implementation("io.ktor:ktor-client-darwin:$ktor")  // <— REQUIRED
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
//            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "com.tagaev.mobileagregatcrm"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.tagaev.mobileagregatcrm"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}


sqldelight {
    database("AppDatabase") {
        packageName = "com.agregat.db"
        schemaOutputDirectory = file("src/commonMain/sqldelight/databases")
        verifyMigrations = true
    }
}
