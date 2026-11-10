package quantum.shared

object LoaderVersionNames {
	private val minecraft_prefix = Regex("^1\\.\\d+(\\.\\d+)?-")

	fun displayName(loader_version: String): String {
		return loader_version.replaceFirst(minecraft_prefix, "")
	}

	fun isStable(loader_version: String): Boolean {
		return !displayName(loader_version).contains("-")
	}
}