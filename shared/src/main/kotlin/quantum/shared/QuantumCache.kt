package quantum.shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

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

	fun fetchText(url: String): String {
		val file_name = "text_${cacheKey(url)}"
		val cached_text = readText(file_name)

		if(cached_text != null) {
			return cached_text
		}

		val fetched_text = QuantumHttp.fetchText(url)
		writeText(file_name, fetched_text)
		return fetched_text
	}

	fun fetchBytes(url: String): ByteArray {
		val file_name = "bytes_${cacheKey(url)}"
		val cached_bytes = readBytes(file_name)

		if(cached_bytes != null) {
			return cached_bytes
		}

		val fetched_bytes = QuantumHttp.fetchBytes(url).body
		writeBytes(file_name, fetched_bytes)
		return fetched_bytes
	}

	fun bytesOrLoad(source: String, load: () -> ByteArray?): ByteArray? {
		val file_name = "entry_${cacheKey(source)}"
		val cached_bytes = readBytes(file_name)

		if(cached_bytes != null) {
			return cached_bytes
		}

		val loaded_bytes = load() ?: return null
		writeBytes(file_name, loaded_bytes)
		return loaded_bytes
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

	private fun cacheKey(text: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
		return digest.joinToString("") { byte -> "%02x".format(byte) }
	}

	private fun cacheFile(file_name: String): Path {
		return QuantumPaths.cache_folder.resolve(file_name)
	}
}