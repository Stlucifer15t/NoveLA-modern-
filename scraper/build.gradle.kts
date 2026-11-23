plugins {
    alias(libs.plugins.dreamora.android.library)
    alias(libs.plugins.dreamora.android.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "my.dreamora.scraper"
}

dependencies {
    implementation(projects.strings)
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.networking)
    implementation(libs.compose.material3.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.jsoup)
    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(libs.timber)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)

    // Lua and YAML support
    implementation(libs.luajvm)
    implementation(libs.snakeyaml)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    androidTestImplementation(libs.test.androidx.espresso.core)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito.kotlin)
}