plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.tukandado.tukandadov2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tukandado.tukandadov2"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            //buildConfigField("String", "BACKEND_URL", "\"http://10.0.2.2:3000/api/\"")
            buildConfigField("String", "BACKEND_URL", "\"https://tukandado-backend.onrender.com/api/\"")
            buildConfigField("boolean", "BYPASS_LOGIN", "false")
            //buildConfigField("boolean", "BYPASS_LOGIN", "true")
            buildConfigField("String", "TEST_JWT", "\"\"")
            //buildConfigField("String",  "TEST_JWT", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY3Y2YxMTJhYTE1ZWMzMmU1NzA1MmMyNyIsInJvbGUiOiJzdXBlcmFkbWluIiwiaWF0IjoxNzU0MTI1MzcxLCJleHAiOjE3NTQxMjgwNzF9.LzMezxHZIzqkxKQRnILsXKUDNhTIIKS7xZ3G3awkJW4\"")
            //buildConfigField("String",  "TEST_JWTB", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6IjY3Y2YxMTJhYTE1ZWMzMmU1NzA1MmMyNyIsInJvbGUiOiJzdXBlcmFkbWluIiwiaWF0IjoxNzU0MTI1MzcxLCJleHAiOjE3NTQxMjgwNzF9.LzMezxHZIzqkxKQRnILsXKUDNhTIIKS7xZ3G3awkJW5\"")
            //buildConfigField("String", "TEST_JWT", "\"\""
        }
        getByName("release") {
            buildConfigField("String", "BACKEND_URL", "\"https://tukandado-backend.onrender.com/api/\"")
            buildConfigField("boolean", "BYPASS_LOGIN", "false")
            buildConfigField("String", "TEST_JWT", "\"\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
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
    implementation(libs.navigation)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation ("androidx.compose.material3:material3:1.3.2")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.ttlock:ttlock:3.5.4")

    implementation("androidx.compose.foundation:foundation:1.5.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation(libs.datastore)
    implementation(libs.androidx.tv.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}