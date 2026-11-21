plugins {
    alias(libs.plugins.noveldokusha.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "my.noveldokusha.extension_compat"

    packaging {
        resources {
            excludes += setOf("META-INF/AL2.0", "META-INF/LGPL2.1", "META-INF/DEPENDENCIES")
        }
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.scraper)
    implementation(projects.networking)
    implementation(projects.strings)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json.okio)
    implementation(libs.okhttp)
    implementation(libs.okhttp.zstd)
    implementation(libs.zstd.kmp.okio)
    implementation(libs.okio)
    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.timber)

    // Legado rule engine: XPath/JSONPath/@js evaluation
    implementation(libs.rhino)
    implementation(libs.json.path)

    // Tachiyomi/Aniyomi extension runtime ABI (vendored sources need these)
    implementation(libs.injekt.core)
    implementation(libs.rxjava1)
    implementation(libs.androidx.preference)
    implementation(libs.okhttp.interceptor.logging)

    // LNReader / Mangayomi JS plugin runtime
    implementation(libs.quickjs.kt)

    testImplementation(libs.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // JVM natives for QuickJS so unit tests execute real plugin code on CI
    testImplementation(libs.quickjs.kt.jvm)
}
