package quantum.shared

object SemanticVersions {
	fun isPreRelease(version: String): Boolean {
		return version.substringBefore("+").contains("-")
	}
}