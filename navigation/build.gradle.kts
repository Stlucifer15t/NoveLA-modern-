plugins {
    alias(libs.plugins.dreamora.android.library)
}

android {
    namespace = "my.dreamora.navigation"
}

dependencies {
    implementation(projects.core)
    implementation(projects.tooling.localDatabase)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}