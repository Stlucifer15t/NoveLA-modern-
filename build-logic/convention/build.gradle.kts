import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "my.dreamora.convention.plugin"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "dreamora.android.application"
            implementationClass =
                "DreamoraAndroidApplicationBestPracticesConventionPlugin" // :)
        }
        register("androidLibrary") {
            id = "dreamora.android.library"
            implementationClass =
                "DreamoraAndroidLibraryBestPracticesConventionPlugin" // ;)
        }
        register("androidLibraryNoHilt") {
            id = "dreamora.android.library.nohilt"
            implementationClass =
                "DreamoraAndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "dreamora.android.compose"
            implementationClass =
                "DreamoraAndroidComposeBestPracticesConventionPlugin" // :)
        }
    }
}
