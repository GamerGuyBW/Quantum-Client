pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		google()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		google()
	}
}

rootProject.name = "Quantum-Client"

include(":app", ":shared")