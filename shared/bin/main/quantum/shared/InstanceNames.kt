package quantum.shared

object InstanceNames {
	const val max_name_length = 20

	fun cleanTyped(typed_name: String): String {
		return typed_name.trimStart().take(max_name_length)
	}

	fun findProblem(name: String, edition: Edition, existing_instances: List<Instance>): String? {
		if(name.isEmpty()) {
			return "Enter a name for the instance."
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

		val is_taken = existing_instances.any { instance -> instance.edition == edition && instance.name == name }
		if(is_taken) {
			return "A ${edition.display_name} instance named `$name` already exists."
		}

		return null
	}
}