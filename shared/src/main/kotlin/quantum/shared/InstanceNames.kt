package quantum.shared

object InstanceNames {
	private const val max_name_bytes = 255

	fun findProblem(name: String, edition: Edition, existing_instances: List<Instance>): String? {
		if(name.isEmpty()) {
			return "Enter a name for the instance."
		}

		if(name.contains("/")) {
			return "Names can't contain /."
		}

		if(name.any { character -> character.isISOControl() }) {
			return "Names can't contain hidden control characters."
		}

		if(name == "." || name == "..") {
			return "Names can't be . or .."
		}

		if(name.toByteArray(Charsets.UTF_8).size > max_name_bytes) {
			return "Names can be at most $max_name_bytes bytes long."
		}

		val is_taken = existing_instances.any { instance -> instance.edition == edition && instance.name == name }
		if(is_taken) {
			return "A ${edition.display_name} instance called $name already exists."
		}

		return null
	}
}