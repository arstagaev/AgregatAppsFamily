import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import kotlin.apply
import com.android.build.api.artifact.SingleArtifact
import java.util.Locale
//import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.gradle.api.file.DuplicatesStrategy

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.0"
    id("app.cash.sqldelight") version "2.1.0"
    id("com.codingfeline.buildkonfig")
    id("com.google.gms.google-services")
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
            isStatic = false
//            linkerOpts("-lsqlite3")
            // 👇 this is the important line
            binaryOption("bundleId", "com.tagaev.trrcrm.shared")
        }
    }

    sourceSets {
        val ktor = "3.3.2" // keep this consistent everywhere

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            implementation("io.ktor:ktor-client-okhttp:${ktor}")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            implementation("androidx.datastore:datastore-preferences:1.1.7") // optional if you prefer DataStore later
            implementation("app.cash.sqldelight:android-driver:2.1.0")

            // Koin for Android
            implementation("io.insert-koin:koin-android:4.1.0")

            //Analytics
            // Import the Firebase BoM
            // Firebase BoM – pin all Firebase libs to a consistent version
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.5.1"))

            // Analytics (Kotlin devs now use the main artifact, not -ktx)
            implementation("com.google.firebase:firebase-analytics")

        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
//            implementation(libs.androidx.lifecycle.viewmodelCompose)
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
            implementation("br.com.devsrsouza.compose.icons:line-awesome:1.1.1")

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
            //qr
            implementation("io.github.ismai117:KScan:0.4.0")
//            implementation("network.chaintech:qr-kit:3.1.3")
//            implementation("io.github.kalinjul.easyqrscan:scanner:0.5.0")

            implementation("io.github.g00fy2.quickie:quickie-bundled:1.11.0")

            // image
            // Compose Multiplatform image loading
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")

            // Network support via Ktor (multiplatform)
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
        }
        iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:2.1.0")
//            implementation("com.squareup.sqldelight:native-driver:1.5.5")
            implementation("io.ktor:ktor-client-darwin:$ktor")  // <— REQUIRED
        }

//        commonTest.dependencies {
//            implementation(libs.kotlin.test)
//        }
//        jvmMain.dependencies {
//            implementation(compose.desktop.currentOs)
////            implementation(libs.kotlinx.coroutinesSwing)
//        }
    }
}

android {
    namespace = "com.tagaev.trrcrm"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    println("COunt version: ${countVersion}")
    defaultConfig {
        applicationId = "com.tagaev.trrcrm"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 8 //
        versionName = version//"1.4.5"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        // Debug: for IDE / local dev, no minify, no shrink
        //./gradlew assembleDebug
        getByName("debug") {
            isMinifyEnabled = true
            isShrinkResources = true
            // no proguardFiles here
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Keep a very basic release as a base (AGP expects it)
        getByName("release") {
            // can be identical to debug, or light minification – your choice
            isMinifyEnabled = true
            isShrinkResources = true
        }

        // Prod: your “real” minified build
        create("prod") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Optional: separate app id so it does not overwrite Play build
            // applicationIdSuffix = ".prod"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

////////////////////
//  androidComponents
////////////////////

androidComponents {
    onVariants(selector().all()) { variant ->
        println("onVariants>>>>> ${variant.buildType}")
        // Only care about prod (and optionally release)
        if (variant.buildType != "prod" && variant.buildType != "release") return@onVariants

        val variantCap = variant.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }

        val versionNameProvider = variant.outputs.single().versionName
        val fileNameProvider = providers.provider {
            val vName = versionNameProvider.orElse("dev").get()
            "MobileTRR_${version}.apk"
        }

        val copyTask = tasks.register<Copy>("rename${variantCap}Apk") {
            notCompatibleWithConfigurationCache("Finalizer wiring touches AGP internals")

            val outDir = layout.buildDirectory.dir("outputs/apk-renamed/${variant.name}")

            from(variant.artifacts.get(SingleArtifact.APK)) {
                // 🔴 important: only copy the APK(s), not metadata
                include("*.apk")
            }
            into(outDir)

            // You now only rename the actual APK
            rename { _ -> fileNameProvider.get() }

            doLast {
                val outFile = outDir.get().asFile.resolve(fileNameProvider.get())
                println(">>> APK for ${variant.name} saved to: ${outFile.absolutePath}")
            }
        }

        tasks.configureEach {
            if (name == "package$variantCap") {
                finalizedBy(copyTask)
            }
        }
    }
}


////////////////////
//  sqldelight
////////////////////
sqldelight {
    databases {
        create("Database") {
            packageName.set("com.agregat.db")
        }
    }
    // Ensure the plugin adds -lsqlite3 automatically for native targets
    linkSqlite.set(true)
}

////////////////////
// buildkonfig
////////////////////
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
val version: String = providers.gradleProperty("VERSION").orNull
    ?: providers.environmentVariable("VERSION").orNull
    ?: localProps.getProperty("VERSION")
    ?: ""
val countVersion: String = providers.gradleProperty("CVERSION").orNull
    ?: providers.environmentVariable("CVERSION").orNull
    ?: localProps.getProperty("CVERSION")
    ?: ""
val isPublish: String = providers.gradleProperty("IS_PUBLISH").orNull
    ?: providers.environmentVariable("IS_PUBLISH").orNull
    ?: localProps.getProperty("IS_PUBLISH")
    ?: ""

// figure out which env we are building based on the Gradle tasks
val requestedTasks = gradle.startParameter.taskNames

val appEnv: String = when {
    requestedTasks.any { it.contains("assembleProd", ignoreCase = true) ||
            it.contains("installProd", ignoreCase = true) } -> "prod"
    requestedTasks.any { it.contains("assembleDebug", ignoreCase = true) ||
            it.contains("installDebug", ignoreCase = true) } -> "debug"
    else -> (project.findProperty("APP_ENV") as String?) ?: "prod"
}

buildkonfig {
    packageName = "com.tagaev.secrets"   // choose any package you like
    objectName = "Secrets"

    // REQUIRED non-flavored defaults
    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", base_url)
        buildConfigField(STRING, "VIEW_TYPE", viewType)
        buildConfigField(STRING, "VERSION", version)
        buildConfigField(STRING, "COUNTVERSION", countVersion)
        buildConfigField(STRING, "IS_PUBLISH", isPublish)
    }
    // If you later use flavors (via buildkonfig.flavor), you STILL keep defaultConfigs above.
    defaultConfigs {
        buildConfigField(STRING, "APP_ENV", appEnv)
    }
}

////////////////////
//  custom apk tasks
////////////////////

tasks.register("buildDebugApk") {
    group = "launchpadAPK"
    description = "Assemble Debug APK (no install, no minify)"

    // this will run the normal debug assemble, same as CLI:
    // ./gradlew :composeApp:assembleDebug
    dependsOn("assembleDebug")
}

tasks.register("buildProdApk") {
    group = "launchpadAPK"
    description = "Assemble Prod APK (minify + shrink + ProGuard, no install)"

    // this will run:
    // ./gradlew :composeApp:assembleProd
    // and your androidComponents(renameProdApk) will still fire and print the path
    dependsOn("assembleProd")
}

// Optional: if you sometimes *do* want to install directly from UI:
tasks.register("installProdApk") {
    group = "launchpadAPK"
    description = "Assemble and install Prod APK to connected device"

    dependsOn("installProd")
}
