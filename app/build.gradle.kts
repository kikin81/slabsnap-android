import com.google.firebase.appdistribution.gradle.firebaseAppDistribution

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.google.services)
    alias(libs.plugins.sort.dependencies)
    jacoco
}

android {
    namespace = "us.kikinsoft.slabsnap"
    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = "us.kikinsoft.slabsnap"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = project.property("version").toString()

        testInstrumentationRunner = "us.kikinsoft.slabsnap.HiltTestRunner"
    }

    buildTypes {
        debug {
            firebaseAppDistribution {
                groups = "internal-testers"
                releaseNotes = System.getenv("APP_DISTRIBUTION_RELEASE_NOTES") ?: ""
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    val jvmTarget = project.property("slabsnap.jvmTarget").toString().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(jvmTarget)
        targetCompatibility = JavaVersion.toVersion(jvmTarget)
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
            it.extensions.configure<JacocoTaskExtension> {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.register("publish") {
    description = "No-op publish task to satisfy semantic-release verification in CI"
    group = "publishing"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "**/ComposableSingletons*.*",
            "**/*_HiltModules*.*",
            "**/*Hilt_*.*",
            "**/*_Factory.*",
            "**/*_MembersInjector.*",
        )

    val debugTree =
        fileTree("${layout.buildDirectory.get()}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes") {
            exclude(fileFilter)
        }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include("jacoco/testDebugUnitTest.exec")
        },
    )
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.firebase.analytics)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.core)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)

    testRuntimeOnly(libs.junit5.platform.launcher)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    kspAndroidTest(libs.hilt.compiler)

    lintChecks(libs.compose.lint.checks)
}
