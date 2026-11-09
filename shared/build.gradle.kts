import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
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
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
}