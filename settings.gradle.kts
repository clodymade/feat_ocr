pluginManagement {
    repositories {
        gradlePluginPortal() // Gradle Plugin Portal for plugins
        google()             // Google's Maven repository
        mavenCentral()       // Maven Central repository
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "feat_ocr"
