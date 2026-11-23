plugins {
    alias(libs.plugins.dreamora.android.library)
    alias(libs.plugins.dreamora.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "my.dreamora.core"
}

dependencies {
    implementation(projects.strings)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.jsoup)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.timber)
    implementation(libs.compose.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference.ktx)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core.ktx)
    testImplementation(libs.robolectric)
    testImplementation("org.json:json:20240303")
}
