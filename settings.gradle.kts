pluginManagement {
    repositories {
        google() // Google repository
        mavenCentral() // Maven Central repository
        maven { url = uri("https://jitpack.io") } // Maven custom repository, se necessario
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // Preferire i repositories qui
    repositories {
        google() // Google repository
        mavenCentral() // Maven Central repository
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "FaceSwapApp2" // Nome del progetto principale
include(":app") // Inclusione del modulo app