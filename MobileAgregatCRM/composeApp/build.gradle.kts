import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import kotlin.apply

//import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.0"
    id("app.cash.sqldelight") version "2.1.0"
    id("com.codingfeline.buildkonfig")
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
            implementation("app.cash.sqldelight:android-driver:2.1.0")

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

//            implementation("com.squareup.sqldelight:runtime:1.5.5")
//            implementation("com.squareup.sqldelight:coroutines-extensions:1.5.5")

            // koin
            implementation("io.insert-koin:koin-core:4.1.0")
            implementation("io.insert-koin:koin-compose:4.1.0")

            // time
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

//            implementation("com.russhwolf:multiplatform-settings-no-arg:1.3.0")
            implementation("com.russhwolf:multiplatform-settings:1.3.0")
            implementation("com.russhwolf:multiplatform-settings-serialization:1.3.0")

            implementation(libs.okio)

            implementation("org.slf4j:slf4j-nop:1.7.36")   // if slf4j-api is 1.7.x
        }
        iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:2.1.0")
//            implementation("com.squareup.sqldelight:native-driver:1.5.5")
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
        versionName = "1.4.2"
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
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
//        debug {
//            isMinifyEnabled = true
//            isShrinkResources = true
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
//
sqldelight {
    databases {
        create("Database") {
            packageName.set("com.agregat.db")
        }
    }
}
// Load local.properties (root of the project)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use(::load)
}

// Resolve the key from (1) Gradle prop, (2) ENV, (3) local.properties
val base_url: String = providers.gradleProperty("BASE_URL").orNull
    ?: providers.environmentVariable("BASE_URL").orNull
    ?: localProps.getProperty("BASE_URL")
    ?: ""
val viewType: String = providers.gradleProperty("VIEW_TYPE").orNull
    ?: providers.environmentVariable("VIEW_TYPE").orNull
    ?: localProps.getProperty("VIEW_TYPE")
    ?: ""

buildkonfig {
    packageName = "com.tagaev.secrets"   // choose any package you like
    objectName = "Secrets"

    // REQUIRED non-flavored defaults
    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", base_url)
        buildConfigField(STRING, "VIEW_TYPE", viewType)
    }
    // If you later use flavors (via buildkonfig.flavor), you STILL keep defaultConfigs above.
}

