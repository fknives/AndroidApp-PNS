plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.serialization)
}

// custom properties
val applicationIdArgument = project.findProperty("applicationId")?.toString()
val applicationVersionCodeArgument = project.findProperty("versionCode")?.toString()?.toIntOrNull()
val baseUrlArgument = System.getenv("PNS_BASE_URL") ?: "http://127.0.0.1:8080/"

android {
    namespace = applicationIdArgument ?: "org.fnives.android.servernotifications"
    compileSdk = 34

    defaultConfig {
        applicationId = applicationIdArgument ?: "org.fnives.android.servernotifications"
        minSdk = 29
        targetSdk = 34
        versionCode = applicationVersionCodeArgument ?: 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "BASE_URL", "\"$baseUrlArgument\"")
    }

    signingConfigs {
        with(maybeCreate("debug")) {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        with(maybeCreate("release")) {
            storeFile = file(System.getenv("PNS_KEYSTORE") ?: "debug.keystore")
            keyAlias = System.getenv("PNS_KEY") ?: ""
            keyPassword = System.getenv("PNS_KEY_PASS") ?: ""
            storePassword = System.getenv("PNS_PASS") ?: ""

        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.fragment)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.serialization.json)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}