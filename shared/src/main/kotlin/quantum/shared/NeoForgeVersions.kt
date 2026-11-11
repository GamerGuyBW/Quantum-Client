package quantum.shared

object NeoForgeVersions {
	private val minecraft_prefix = Regex("^(1\\.\\d+(\\.\\d+)?)-")

	fun gameVersion(neoforge_version: String): String? {
		val prefix_match = minecraft_prefix.find(neoforge_version)
		if(prefix_match != null) {
			return prefix_match.groupValues[1]
		}

		val build_tag = neoforge_version.substringAfter("+", "")
		val number_part = neoforge_version.substringBefore("-").substringBefore("+")
		val numbers = number_part.split(".").mapNotNull { part -> part.toIntOrNull() }

		if(numbers.size < 3) {
			return null
		}

		val major = numbers[0]
		val minor = numbers[1]
		val patch = numbers[2]

		if(major >= 26) {
			if(numbers.size < 4) {
				return null
			}

			if(build_tag.isNotEmpty()) {
				return "$major.$minor-$build_tag"
			}

			return if(patch == 0) "$major.$minor" else "$major.$minor.$patch"
		}

		return if(minor == 0) "1.$major" else "1.$major.$minor"
	}
}