@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.android.application")
    alias(libs.plugins.org.jetbrains.kotlin.android)
    id("org.jetbrains.compose")
}

// 1. 优先声明生成任务与配置

// 新增独立的桌面端运行环境配置 (用于支持桌面端 Fat JAR)
val desktopRuntime by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    // 关键修正：添加属性以协助 Gradle 在解析 Compose/Skiko 依赖时选择 AWT (Desktop) 变体
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.LIBRARY))
        attribute(org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.attribute, org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.jvm)
        attribute(Attribute.of("ui", String::class.java), "awt")
    }
}

// 生成桌面端胖包任务 (Containerized Jar)
val packageDesktopJar by tasks.registering(Zip::class) {
    dependsOn("compileDebugKotlin")
    dependsOn("compileDebugJavaWithJavac")
    
    archiveFileName.set("desktop-launcher.jar")
    val assetsDir = layout.buildDirectory.dir("generated/universal-assets")
    destinationDirectory.set(assetsDir)
    
    from(layout.buildDirectory.dir("intermediates/javac/debug/classes")) { include("**/*.class") }
    from(layout.buildDirectory.dir("tmp/kotlin-classes/debug")) { include("**/*.class") }

    // 关键修正：从 desktopRuntime 配置中抓取全量依赖并合并到 JAR 容器内
    val runtimeConfig = configurations.getByName("desktopRuntime")
    from(runtimeConfig.map { file ->
        val fileName = file.name
        val isThirdPartyJar = fileName.endsWith(".jar") && 
            !fileName.contains("shared") && 
            !fileName.contains("android") && 
            !fileName.contains("androidx") &&
            !fileName.contains("kotlin-stdlib")
        if (isThirdPartyJar) zipTree(file) else files()
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// 2. 安卓配置块

android {
    namespace = "com.zzx.plugin.scriptexecutor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zzx.plugin.scriptexecutor"
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // 针对新版 AGP 的资产动态挂载
    sourceSets.getByName("main") {
        resources.srcDirs("src/main/assets") 
        assets.srcDir("src/main/assets")
        
        // 挂载生成的桌面端胖包
        assets.srcDir(packageDesktopJar.map { it.destinationDirectory })
    }
}

// 3. 任务生命周期对齐
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(packageDesktopJar)
}

dependencies {
    // 基础依赖 (compileOnly)
    compileOnly(project(":shared"))
    compileOnly(libs.androidx.core.ktx)
    compileOnly(libs.androidx.appcompat)
    compileOnly(libs.material)
    compileOnly(compose.runtime)
    compileOnly(compose.foundation)
    compileOnly(compose.material3)
    compileOnly("com.google.code.gson:gson:2.10.1")

    // 桌面端运行环境 (用于 Fat JAR 打包)
    desktopRuntime(libs.androidx.core.ktx)
    desktopRuntime(libs.androidx.appcompat)
    desktopRuntime(libs.material)
    desktopRuntime(compose.runtime)
    desktopRuntime(compose.foundation)
    desktopRuntime(compose.material3)
    desktopRuntime("com.google.code.gson:gson:2.10.1")
}
