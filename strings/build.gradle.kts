plugins {
    alias(libs.plugins.dreamora.android.library.nohilt)
}

android {
    namespace = "my.dreamora.strings"
    androidResources {
        resourcePrefix = ""
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
}