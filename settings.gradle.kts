pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Bookiba"
include(":app")
include(":core:designsystem")
include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:common")
include(":feature:onboarding")
include(":feature:auth")
include(":feature:home")
include(":feature:explore")
include(":feature:reels")
include(":feature:bookdetails")
include(":feature:wishlist")
include(":feature:cart")
include(":feature:checkout")
include(":feature:orders")
include(":feature:profile")
include(":feature:notifications")
include(":feature:support")
