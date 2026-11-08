import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm")
	id("org.jetbrains.kotlin.plugin.compose")
	id("org.jetbrains.compose")
}

java {
	sourceCompatibility = JavaVersion.VERSION_26
	targetCompatibility = JavaVersion.VERSION_26
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_26
	}
}

dependencies {
	implementation(compose.desktop.currentOs)
}

compose.desktop {
	application {
		mainClass = "quantum.client.MainKt"
	}
}