plugins {
    `kotlin-dsl`
}

group = "co.booknook.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.application)
    compileOnly(libs.android.library)
    compileOnly(libs.kotlin.android)
    compileOnly(libs.kotlin.compose)
}
