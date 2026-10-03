// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

import io.gitlab.arturbosch.detekt.Detekt
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverReportFilter
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
    alias(libs.plugins.licensee)
    alias(libs.plugins.room)
}

android {
    namespace = "com.qtekfun.ultimatetasks"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.qtekfun.ultimatetasks"
        minSdk = 26
        targetSdk = 37
        // Derived from appVersion in gradle.properties once releases start (T31).
        versionCode = 1
        versionName = providers.gradleProperty("appVersion").get()
    }

    // Reproducible builds (F-Droid): no Google-encrypted dependency blob in the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            // The git commit is not part of the APK: a build from a source tarball must match.
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        // The version shown at the foot of Settings.
        buildConfig = true
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = true
    }

    androidResources {
        generateLocaleConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java", "src/androidTest/java")
}

tasks.withType<Detekt>().configureEach {
    // Match the project bytecode level; detekt defaults to the JDK running Gradle.
    jvmTarget = "17"
}

ktlint {
    version.set(libs.versions.ktlint)
}

// Coverage policy (CLAUDE.md): >= 85% over domain/data/sync, 100% on the sync queue, the
// conflict resolver, the recurrence engine and the reminder planner. Generated code and pure
// Compose UI are excluded.
val coveredPackages = listOf(
    "com.qtekfun.ultimatetasks.domain",
    "com.qtekfun.ultimatetasks.data",
    "com.qtekfun.ultimatetasks.sync"
)
val criticalPackages = listOf(
    "com.qtekfun.ultimatetasks.sync.queue",
    "com.qtekfun.ultimatetasks.sync.conflict",
    "com.qtekfun.ultimatetasks.domain.recurrence",
    "com.qtekfun.ultimatetasks.domain.reminders"
)

/**
 * Generated code and pure Compose UI, excluded from coverage (CLAUDE.md). Applied to each report
 * variant: variant filters replace the global ones instead of adding to them.
 */
fun KoverReportFilter.generatedAndUiCode() {
    packages("com.qtekfun.ultimatetasks.ui", "dagger.hilt.internal", "hilt_aggregated_deps")
    classes(
        "*.R",
        "*.R$*",
        "*.BuildConfig",
        "*Hilt_*",
        "*_HiltModules*",
        "*_Factory",
        "*_Factory$*",
        "*_MembersInjector",
        // Room
        "*_Impl",
        "*_Impl$*",
        // Kotlin compatibility bridges for interface default methods
        "*\$DefaultImpls",
        "*ComposableSingletons*"
    )
    annotatedBy(
        "androidx.compose.ui.tooling.preview.Preview",
        "dagger.Module",
        "dagger.hilt.android.HiltAndroidApp",
        "*Generated*"
    )
}

kover {
    currentProject {
        createVariant("critical") {
            add("debug")
        }
    }

    reports {
        total {
            filters {
                excludes { generatedAndUiCode() }
                includes {
                    packages(coveredPackages)
                }
            }
            verify {
                rule("domain, data and sync") {
                    minBound(85)
                }
            }
        }

        variant("critical") {
            filters {
                excludes { generatedAndUiCode() }
                includes {
                    packages(criticalPackages)
                }
            }
            verify {
                rule("sync queue, conflict resolver, recurrence and reminders") {
                    minBound(100, CoverageUnit.LINE)
                    minBound(100, CoverageUnit.BRANCH)
                }
            }
        }
    }
}

tasks.named("koverVerify") {
    dependsOn("koverVerifyCritical")
}

tasks.named("check") {
    dependsOn("koverVerify")
}

// Only GPL-3.0-compatible free licenses may ship in the APK. Anything else,
// including dependencies without a declared license, fails the build.
// Add other GPL-3.0-compatible SPDX ids (MIT, BSD-2-Clause, BSD-3-Clause,
// ISC...) only when a dependency needs them; licensee warns about unused ones.
licensee {
    allow("Apache-2.0")
}

// Google Play Services, Firebase and Crashlytics are banned outright (F-Droid
// rules in CLAUDE.md), regardless of what license they declare.
val checkForbiddenDependencies = tasks.register("checkForbiddenDependencies") {
    group = "verification"
    description =
        "Fails if a runtime classpath contains Google Play Services, Firebase or Crashlytics."
    val forbiddenGroupPrefixes = listOf(
        "com.google.android.gms",
        "com.google.firebase",
        "com.crashlytics",
        "io.fabric"
    )
    val runtimeModules = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath").map { name ->
        configurations.named(name).flatMap { it.incoming.resolutionResult.rootComponent }
    }
    doLast {
        val modules = mutableSetOf<String>()
        val seen = mutableSetOf<ResolvedComponentResult>()
        val pending = ArrayDeque(runtimeModules.map { it.get() })
        while (pending.isNotEmpty()) {
            val component = pending.removeFirst()
            if (seen.add(component)) {
                (component.id as? ModuleComponentIdentifier)?.let {
                    modules.add(it.moduleIdentifier.toString())
                }
                component.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .forEach { pending.add(it.selected) }
            }
        }
        val offenders = modules
            .filter { module -> forbiddenGroupPrefixes.any { module.startsWith(it) } }
            .sorted()
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Forbidden non-free dependencies found: ${offenders.joinToString()}"
            )
        }
    }
}

tasks.named("check") {
    dependsOn(checkForbiddenDependencies)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.androidx.work.runtime)
    ksp(libs.room.compiler)

    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    // Host JVM build of the bundled SQLite, so Room runs in local unit tests.
    testImplementation(libs.sqlite.bundled.jvm)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.okhttp.mockwebserver.junit5)
}
