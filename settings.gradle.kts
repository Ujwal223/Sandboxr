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

rootProject.name = "sandboxr"

// Core Multi-Module Architecture
// - :app: Top-level Sandboxr home entrypoint, environment manager & settings
// - :launcher: GrapheneOS Launcher3 port with Liquid Glass design system & full AOSP engine
// - :virtual-core: NewBlackBox zero-root userspace virtualization engine with native ShadowHook/ByteHook
// - :network: Headless RethinkDNS firestack local Tun network engine & in-RAM ad-blocker
// - :aidl: IPC contract between Core FOSS and SANDBOXR Pro
include(":app")
include(":launcher")
include(":virtual-core")
include(":network")
include(":aidl")
