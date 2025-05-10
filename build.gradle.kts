plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.10" apply false
    id("com.android.application") version "8.6.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        // Repository MediaPipe (per librerie aggiuntive)
        maven {
            url = uri("https://storage.googleapis.com/maven-central-prod/")
        }
    }
}