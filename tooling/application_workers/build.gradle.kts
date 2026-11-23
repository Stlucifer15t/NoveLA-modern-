plugins {
    alias(libs.plugins.dreamora.android.library)
    alias(libs.plugins.dreamora.android.compose)
}

android {
    namespace = "my.dreamora.tooling.application_workers"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.navigation)
    implementation(projects.networking)
    implementation(projects.scraper)
    implementation(projects.tooling.localDatabase)
    implementation(projects.tooling.backupCreate)
    implementation(projects.tooling.epubParser)

    implementation(libs.timber)
    implementation(libs.androidx.workmanager)
    implementation(libs.hilt.workmanager)

    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.core.ktx)
}
