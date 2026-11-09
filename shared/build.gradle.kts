import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	kotlin("jvm")
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