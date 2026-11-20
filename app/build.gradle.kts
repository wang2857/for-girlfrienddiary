import java.util.Properties
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 正式签名信息放在 keystore/release.properties（不入库，见 .gitignore；模板是 .example）
val releaseSigning = Properties().apply {
    val file = rootProject.file("keystore/release.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.synn.diary"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.synn.diary"
        minSdk = 26
        targetSdk = 36
        versionCode = 78
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // 只打 64 位包：vivo 等国内商店早已只收 64 位，32 位 so 留着只是白白增大体积
            abiFilters += "arm64-v8a"
        }
    }

    // 用项目内的 debug keystore，而不是默认的 ~/.android/debug.keystore：
    // 一份签名固定下来，覆盖安装、卸载重装都不会因为换签名失败，也便于沙盒/CI 环境构建。
    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        // 正式包用自己的 RSA 4096 key；密钥只留本机，仓库里没有。
        // 没放 release.properties 时 release 会回落到调试签名，照样能构建。
        if (releaseSigning.getProperty("storePassword") != null) {
            create("release") {
                storeFile = rootProject.file(
                    "keystore/" + releaseSigning.getProperty("storeFile", "release.keystore"),
                )
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            // 正式签名（keystore/release.keystore），和调试包是两个不同的应用，可共存
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // android.util.Log 这类桩在 JVM 测试里直接返回默认值，别让它们把测试打断
        unitTests.isReturnDefaultValues = true
        // Robolectric 需要 resources
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        // 只打 arm64 是按 vivo 的 64 位要求有意为之，不是漏了 x86_64
        disable += "ChromeOsAbiSupport"
    }

    packaging {
        resources {
            // 只挡重复的许可证别名文件，别挡真正的许可正文：
            // androidx 的 META-INF/androidx/**/LICENSE.txt 必须留在 APK 里
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Robolectric 运行时会把 android-all 下到本地 Maven 仓库；
// 沙盒里默认的 ~/.m2 不可写，指向工程内的目录。
tasks.withType<Test>().configureEach {
    val home = layout.buildDirectory.dir("test-home").get().asFile.apply { mkdirs() }
    // 沙盒里 ~ 不可写：Robolectric 要在 user.home 下建下载锁、下载 android-all
    systemProperty("user.home", home.absolutePath)
    systemProperty("maven.repo.local", layout.buildDirectory.dir("robolectric-m2").get().asFile.absolutePath)
}

ksp {
    // 导出 Room schema，迁移 SQL 要照着它核对，不能靠肉眼猜
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // enforcedPlatform：Coil 会带进更新的 Compose，不强制就会被顶到 AGP 9 才支持的那一档
    implementation(enforcedPlatform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work)

    // 数据库加密：SQLCipher 的 Android 封装，Room 通过 openHelperFactory 接入
    implementation(libs.sqlcipher.android)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

/**
 * 64 位合规自检（vivo 64 位适配指南 / Google 64 位要求）：
 * APK 里一旦出现 armeabi / armeabi-v7a / x86 的 so，就构建失败。
 * 手工验也行，但容易忘；做成任务后每次 build 都会兜住。
 */
tasks.register("check64Bit") {
    dependsOn("assembleDebug")
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        require(apk.exists()) { "APK 不存在: ${apk.absolutePath}" }

        val thirtyTwoBit = ZipFile(apk).use { zip ->
            zip.entries().asSequence()
                .map { it.name }
                .filter { name ->
                    name.endsWith(".so") &&
                        (name.startsWith("lib/armeabi/") ||
                            name.startsWith("lib/armeabi-v7a/") ||
                            name.startsWith("lib/x86/"))
                }
                .toList()
        }
        if (thirtyTwoBit.isNotEmpty()) {
            throw GradleException("发现 32 位原生库，64 位合规检查失败：$thirtyTwoBit")
        }

        ZipFile(apk).use { zip ->
            val hasArm64 = zip.entries().asSequence().any { it.name.startsWith("lib/arm64-v8a/") }
            val nativeCount = zip.entries().asSequence().count { it.name.endsWith(".so") }
            if (nativeCount > 0 && !hasArm64) {
                throw GradleException("APK 含原生库但没有 arm64-v8a，不满足 64 位要求")
            }
        }
        println("64 位检查通过：无 32 位 so")
    }
}
