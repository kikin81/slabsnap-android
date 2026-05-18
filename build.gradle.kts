// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.firebase.appdistribution) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.sort.dependencies)
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("$projectDir/.editorconfig")
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_trailing-comma-on-call-site" to "enabled",
                    "ktlint_standard_trailing-comma-on-declaration-site" to "enabled",
                    "ij_kotlin_allow_trailing_comma" to "true",
                    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
                    "ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "2",
                    "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than" to "2",
                ),
            )
            .customRuleSets(
                listOf(
                    "io.nlopez.compose.rules:ktlint:0.5.8",
                ),
            )
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .editorConfigOverride(
                mapOf(
                    "ktlint_code_style" to "android_studio",
                    "ktlint_standard_trailing-comma-on-call-site" to "enabled",
                    "ktlint_standard_trailing-comma-on-declaration-site" to "enabled",
                    "ij_kotlin_allow_trailing_comma" to "true",
                    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
                ),
            )
    }
}
