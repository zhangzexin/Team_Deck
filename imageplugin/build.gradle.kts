@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    id("com.android.application")
    alias(libs.plugins.org.jetbrains.kotlin.android)
    id("org.jetbrains.compose")
}

android {
    namespace = "com.example.imageplugin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zzx.plugin.image"
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val generatePluginProperties by tasks.registering {
    val outputFile = layout.buildDirectory.file("generated/plugin.properties")
    outputs.file(outputFile)
    doLast {
        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeText("plugin.mainClass=com.zzx.plugin.ImagePlugin\n")
    }
}

tasks.register<Zip>("packageUniversalApk") {
    dependsOn(generatePluginProperties)
    archiveFileName.set("universal-image-plugin.apk")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    // 1. 包含编译后的 .class 文件 (用于 Desktop JVM 加载)
    from(layout.buildDirectory.dir("intermediates/javac/debug/classes"))
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debug"))
    
    // 2. 包含生成的插件元数据
    from(generatePluginProperties.map { it.outputs.files.singleFile }) {
        into("/")
    }
    
    // 3. 包含 Android APK 的全部内容 (DEX, 资源, 清单文件)
    dependsOn("assembleDebug")
    from(zipTree(layout.buildDirectory.file("outputs/apk/debug/imageplugin-debug.apk"))) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    compileOnly(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
