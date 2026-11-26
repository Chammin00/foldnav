plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}

val localAppData = System.getenv("LOCALAPPDATA")
val isCloudDrive = rootDir.absolutePath.contains("My Drive", ignoreCase = true)
val customBuildRoot = providers.gradleProperty("foldnavBuildDir").orNull
    ?: if (isCloudDrive && !localAppData.isNullOrBlank()) "$localAppData\\FoldNavGradleBuild" else null

if (customBuildRoot != null) {
    layout.buildDirectory.set(file("$customBuildRoot\\root"))
    subprojects {
        layout.buildDirectory.set(file("$customBuildRoot\\${project.name}"))
    }
}
