plugins {
    alias(libs.plugins.dreamora.android.library)
    alias(libs.plugins.dreamora.android.compose)
}

android {
    namespace = "my.dreamora.tooling.texttospeech"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.test.junit)

    implementation(libs.timber)
}