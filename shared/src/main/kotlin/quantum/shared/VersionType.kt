package quantum.shared

enum class VersionType(val api_name: String, val display_name: String) {
	RELEASE("release", "Release"),
	SNAPSHOT("snapshot", "Snapshot"),
	BETA("old_beta", "Beta"),
	ALPHA("old_alpha", "Alpha");

	companion object {
		fun fromApiName(api_name: String): VersionType? {
			return entries.firstOrNull { version_type -> version_type.api_name == api_name }
		}
	}
}