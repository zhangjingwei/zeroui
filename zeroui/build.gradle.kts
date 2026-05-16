import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

val isJitPackBuild = providers.environmentVariable("JITPACK").isPresent
val publicationGroup = if (isJitPackBuild) {
    listOfNotNull(
        providers.environmentVariable("GROUP").orNull,
        providers.environmentVariable("ARTIFACT").orNull,
    ).joinToString(".")
} else {
    "com.zero.zero-tools"
}
val publicationVersion = if (isJitPackBuild) {
    providers.environmentVariable("VERSION").orNull ?: "0.1.0"
} else {
    "0.1.0"
}

group = publicationGroup
version = publicationVersion

val projectUrl = "https://github.com/zhangjingwei/zeroui"

android {
    namespace = "com.zero.zero_tools.zeroui"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (!name.contains("UnitTest") && !name.contains("AndroidTest")) {
        compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
    }
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime.saveable)
    testImplementation(libs.junit)
    // Android's android.jar ships org.json as Stub! in unit tests — provide a real impl for JVM tests
    testImplementation("org.json:json:20240303")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = publicationGroup
                artifactId = "zeroui"
                version = publicationVersion

                pom {
                    name.set("ZeroUI")
                    description.set("Android Server-Driven UI runtime for JSON-described Compose screens.")
                    url.set(projectUrl)

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("$projectUrl/blob/main/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.set("zhangjingwei")
                            name.set("zhangjingwei")
                        }
                    }

                    scm {
                        url.set(projectUrl)
                        connection.set("scm:git:$projectUrl.git")
                        developerConnection.set("scm:git:$projectUrl.git")
                    }
                }
            }
        }
    }
}
