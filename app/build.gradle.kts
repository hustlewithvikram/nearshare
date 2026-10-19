plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val sharedKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val sharedKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val sharedKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val sharedKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasSharedSigning = !sharedKeystorePath.isNullOrBlank() &&
    !sharedKeystorePassword.isNullOrBlank() &&
    !sharedKeyAlias.isNullOrBlank() &&
    !sharedKeyPassword.isNullOrBlank()
val supabaseUrl = providers.gradleProperty("SUPABASE_URL")
    .orElse(providers.environmentVariable("SUPABASE_URL")).getOrElse("")
val supabaseAnonKey = providers.gradleProperty("SUPABASE_ANON_KEY")
    .orElse(providers.environmentVariable("SUPABASE_ANON_KEY")).getOrElse("")
fun String.asBuildConfigLiteral(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.vikram.nearshare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vikram.nearshare"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "SUPABASE_URL", supabaseUrl.asBuildConfigLiteral())
        buildConfigField("String", "SUPABASE_ANON_KEY", supabaseAnonKey.asBuildConfigLiteral())
    }

    signingConfigs {
        if (hasSharedSigning) {
            create("shared") {
                storeFile = file(sharedKeystorePath!!)
                storePassword = sharedKeystorePassword
                keyAlias = sharedKeyAlias
                keyPassword = sharedKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (hasSharedSigning) signingConfig = signingConfigs.getByName("shared")
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            if (hasSharedSigning) signingConfig = signingConfigs.getByName("shared")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
