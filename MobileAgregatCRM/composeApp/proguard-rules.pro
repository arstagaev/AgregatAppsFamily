# Kotlinx Serialization (keep generated serializers)
-keep class **$$serializer { *; }
-keep @kotlinx.serialization.Serializable class ** { *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Koin (safe for DSL definitions)
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Decompose / Essenty (no reflection, just silence warns)
-dontwarn com.arkivanov.**

# SQLDelight / Okio / Ktor / OkHttp
-dontwarn app.cash.sqldelight.**
-dontwarn okio.**
-dontwarn okhttp3.**
-dontwarn io.ktor.**
