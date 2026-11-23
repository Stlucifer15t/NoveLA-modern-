plugins {
    alias(libs.plugins.dreamora.android.library)
    alias(libs.plugins.dreamora.android.compose)
}

android {
    namespace = "my.dreamora.tooling.backup_restore"
}

dependencies {
    implementation(projects.core)
    implementation(projects.coreui)
    implementation(projects.strings)
    implementation(projects.data)
    implementation(projects.tooling.localDatabase)
    implementation(projects.networking)

    implementation(libs.timber)
    implementation(libs.compose.androidx.activity)
    implementation(libs.compose.material3.android)
    implementation(libs.compose.androidx.material.icons.extended)
    implementation(libs.compose.coil)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.androidx.core.ktx)
    testImplementation(libs.robolectric)
    testImplementation("org.json:json:20240303")
}