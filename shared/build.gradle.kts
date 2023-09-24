plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()

    jvm("desktop")

//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//         baseName = "shared"
//            isStatic = true
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                api(compose.components.resources)
                api(compose.ui)
                api(compose.preview)
                api(libs.okhttp)
                api(libs.google.gson)

//                api("com.arkivanov.mvikotlin:mvikotlin:3.1.0")
//                api("com.arkivanov.mvikotlin:mvikotlin-main:3.1.0")
//                api("com.arkivanov.mvikotlin:mvikotlin-extensions-coroutines:3.1.0")
//                api("com.arkivanov.decompose:decompose:1.0.0-compose-experimental")
//                api("com.arkivanov.decompose:extensions-compose-jetbrains:1.0.0-compose-experimental")
//                api("com.arkivanov.essenty:lifecycle:1.0.0")
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.androidx.activity.compose)
                api(libs.androidx.appcompat)
                api(libs.androidx.core.ktx)
//                api("androidx.compose.ui:ui")
//                api("androidx.compose.ui:ui-graphics")
//                api("androidx.compose.ui:ui-tooling-preview")
            }
        }
        val desktopMain by getting {
            dependencies {
                api(compose.desktop.common)
                api(libs.okhttp3.mockwebserver)
                api(libs.jmdns)
            }
        }

        //暂不支持IOS
//        val iosX64Main by getting
//        val iosArm64Main by getting
//        val iosSimulatorArm64Main by getting
//        val iosMain by creating {
//            dependsOn(commonMain)
//            iosX64Main.dependsOn(this)
//            iosArm64Main.dependsOn(this)
//            iosSimulatorArm64Main.dependsOn(this)
//        }

    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.myapplication.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}