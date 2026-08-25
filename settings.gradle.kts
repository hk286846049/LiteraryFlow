pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven{ url = uri("https://maven.aliyun.com/repository/google/") }
        maven{ url = uri("https://maven.aliyun.com/repository/public/") }
        gradlePluginPortal()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }

    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven{ url = uri("https://maven.aliyun.com/repository/google/") }
        maven{ url = uri("https://maven.aliyun.com/repository/public/") }
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }

    }
}

rootProject.name = "LiteraryFlow"
include(":app")
include(":fast")
include(":studyDemo")
