plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

val webUiDir = rootProject.projectDir.resolve("web-ui")
val webUiAssetsDir = projectDir.resolve("src/main/assets/web_ui")

val buildWebUi by tasks.registering(Exec::class) {
    workingDir.set(webUiDir)
    commandLine("npm", "install")
}

val buildWebUiBundle by tasks.registering(Exec::class) {
    dependsOn(buildWebUi)
    workingDir.set(webUiDir)
    commandLine("npm", "run", "build")
}

val copyWebUiAssets by tasks.registering(Copy::class) {
    dependsOn(buildWebUiBundle)
    from(webUiDir.resolve("dist"))
    into(webUiAssetsDir)
}

tasks.matching { it.name.contains("assemble", true) || it.name == "preBuild" }.configureEach {
    dependsOn(copyWebUiAssets)
}

android {
    namespace = "com.aji.wa_gateway"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aji.wa_gateway"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES,INDEX.LIST}"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-cio:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-gson:2.3.7")
    implementation("io.ktor:ktor-server-websockets:2.3.7")

    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20230926-2.0.0")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test.ext:junit:1.1.5")
}
