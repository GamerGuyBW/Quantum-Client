package quantum.shared

object NameRules {
	const val max_name_length = 20

	fun cleanTyped(typed_name: String): String {
		return typed_name.trimStart().take(max_name_length)
	}

	fun findProblem(name: String, kind: String, edition: Edition, taken_names: List<String>): String? {
		if(name.isEmpty()) {
			return "Enter a name for the $kind."
		}

		if(name.length > max_name_length) {
			return "Names cannot be longer than $max_name_length characters."
		}

		if(name.contains("/")) {
			return "Names cannot contain `/`."
		}

		if(name.any { character -> character.isISOControl() }) {
			return "Names cannot contain hidden control characters."
		}

		if(name == "." || name == "..") {
			return "Names cannot be `.` or `..`."
		}

		if(name in taken_names) {
			return "A ${edition.display_name} $kind named `$name` already exists."
		}

		return null
	}
}