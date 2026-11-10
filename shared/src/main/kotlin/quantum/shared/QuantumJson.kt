package quantum.shared

import kotlinx.serialization.json.Json

object QuantumJson {
	val json = Json {
		prettyPrint = true
		prettyPrintIndent = "\t"
		encodeDefaults = true
		ignoreUnknownKeys = true
	}

	val compact_json = Json {
		encodeDefaults = true
		ignoreUnknownKeys = true
	}
}