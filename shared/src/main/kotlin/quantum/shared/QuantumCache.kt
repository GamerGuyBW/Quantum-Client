package quantum.shared

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object QuantumCache {
	fun clear() {
		val cache_folder = QuantumPaths.cache_folder

		if(Files.exists(cache_folder)) {
			Files.walk(cache_folder).use { path_stream ->
				path_stream
					.sorted(Comparator.reverseOrder())
					.forEach { path -> Files.deleteIfExists(path) }
			}
		}

		Files.createDirectories(cache_folder)
	}

	fun readText(file_name: String): String? {
		val cache_file = cacheFile(file_name)
		return if(Files.exists(cache_file)) Files.readString(cache_file) else null
	}

	fun writeText(file_name: String, text: String) {
		AtomicFiles.writeText(cacheFile(file_name), text)
	}

	fun readBytes(file_name: String): ByteArray? {
		val cache_file = cacheFile(file_name)
		return if(Files.exists(cache_file)) Files.readAllBytes(cache_file) else null
	}

	fun writeBytes(file_name: String, bytes: ByteArray) {
		AtomicFiles.writeBytes(cacheFile(file_name), bytes)
	}

	fun textOrFetch(file_name: String, url: String): String {
		val cached_text = readText(file_name)
		if(cached_text != null) {
			return cached_text
		}

		val fetched_text = QuantumHttp.fetchText(url)
		writeText(file_name, fetched_text)
		return fetched_text
	}

	@Synchronized
	fun appendLine(file_name: String, line: String) {
		Files.writeString(cacheFile(file_name), line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
	}

	@Synchronized
	fun readLines(file_name: String): List<String> {
		val cache_file = cacheFile(file_name)
		if(Files.notExists(cache_file)) {
			return emptyList()
		}

		return Files.readAllLines(cache_file).filter { line -> line.isNotBlank() }
	}

	fun safeName(text: String): String {
		return URLEncoder.encode(text, StandardCharsets.UTF_8)
	}

	private fun cacheFile(file_name: String): Path {
		return QuantumPaths.cache_folder.resolve(file_name)
	}
}