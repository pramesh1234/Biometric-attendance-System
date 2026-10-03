pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "Attendance"
include(":app", ":core:model", ":core:common", ":core:designsystem", ":core:navigation", ":feature:onboarding", ":feature:admin", ":feature:staff")

include(":core:domain", ":core:database", ":core:data", ":core:device")

include(":core:testing")
