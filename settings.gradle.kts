pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    // Vamos remover a linha que estava aqui (repositoriesMode.set...)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "HandTrackify"
include(":app")
