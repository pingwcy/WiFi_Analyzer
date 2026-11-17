import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 读取本地签名配置（项目根目录 keystore.properties，已被 .gitignore 忽略）。
// 用于本地 release 构建时自动签名；CI 则通过 GitHub Secrets 注入环境变量。
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

android {
    namespace = "top.pingwcy.wifianalyzer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "top.pingwcy.wifianalyzer"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // 签名信息来源优先级：环境变量（CI）> 本地 keystore.properties > 跳过签名。
        val envPath = System.getenv("RELEASE_KEYSTORE_PATH")
        val envPassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
        val envAlias = System.getenv("RELEASE_KEY_ALIAS")
        val envKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")

        val resolvedStoreFile = envPath ?: keystoreProperties.getProperty("storeFile")
        val resolvedStorePassword = envPassword ?: keystoreProperties.getProperty("storePassword")
        val resolvedKeyAlias = envAlias ?: keystoreProperties.getProperty("keyAlias")
        val resolvedKeyPassword = envKeyPassword ?: keystoreProperties.getProperty("keyPassword")

        if (!resolvedStoreFile.isNullOrBlank() &&
            !resolvedStorePassword.isNullOrBlank() &&
            !resolvedKeyAlias.isNullOrBlank() &&
            !resolvedKeyPassword.isNullOrBlank()
        ) {
            create("release") {
                storeFile = rootProject.file(resolvedStoreFile)
                storePassword = resolvedStorePassword
                keyAlias = resolvedKeyAlias
                keyPassword = resolvedKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            optimization {
                enable = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro" )// 确保这一行存在
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.accompanist.permissions)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    dependencies { implementation("androidx.navigation:navigation-compose:2.10.2")}
}
