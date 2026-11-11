package quantum.shared

object VersionRanges {
	fun describe(supported_ids: Collection<String>, manifest: VersionManifest): List<String> {
		val supported_set = supported_ids.toSet()

		return VersionType.entries.mapNotNull { version_type ->
			val ordered_versions = manifest.versions
				.filter { version -> VersionType.fromApiName(version.type) == version_type }
				.sortedBy { version -> version.release_time }

			val ranges = mutableListOf<String>()
			var range_start: String? = null
			var range_end: String? = null

			for(version in ordered_versions) {
				if(version.id in supported_set) {
					if(range_start == null) {
						range_start = version.id
					}
					range_end = version.id
				} else if(range_start != null && range_end != null) {
					ranges.add(rangeText(range_start, range_end))
					range_start = null
					range_end = null
				}
			}

			if(range_start != null && range_end != null) {
				ranges.add(rangeText(range_start, range_end))
			}

			if(ranges.isEmpty()) null else "${version_type.display_name}: ${ranges.joinToString(", ")}"
		}
	}

	private fun rangeText(range_start: String, range_end: String): String {
		return if(range_start == range_end) range_start else "$range_start – $range_end"
	}
}