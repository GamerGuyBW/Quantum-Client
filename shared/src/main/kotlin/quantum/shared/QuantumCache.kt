package quantum.shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

object QuantumCache {
	private val file_locks = ConcurrentHashMap<String, ReentrantLock>()

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

		return withFileLock(file_name) {
			val cached_text = readText(file_name)

			if(cached_text != null) {
				cached_text
			} else {
				val fetched_text = QuantumHttp.fetchText(url)
				writeText(file_name, fetched_text)
				fetched_text
			}
		}
	}

	fun fetchBytes(url: String): ByteArray {
		val file_name = "bytes_${cacheKey(url)}"

		return withFileLock(file_name) {
			val cached_bytes = readBytes(file_name)

			if(cached_bytes != null) {
				cached_bytes
			} else {
				val fetched_bytes = QuantumHttp.fetchBytes(url).body
				writeBytes(file_name, fetched_bytes)
				fetched_bytes
			}
		}
	}

	fun bytesOrLoad(source: String, load: () -> ByteArray?): ByteArray? {
		val file_name = "entry_${cacheKey(source)}"

		return withFileLock(file_name) {
			val cached_bytes = readBytes(file_name)

			if(cached_bytes != null) {
				cached_bytes
			} else {
				val loaded_bytes = load()
				if(loaded_bytes != null) {
					writeBytes(file_name, loaded_bytes)
				}
				loaded_bytes
			}
		}
	}

	fun readCachedText(url: String): String? {
		return readText("text_${cacheKey(url)}")
	}

	fun readCachedBytes(url: String): ByteArray? {
		return readBytes("bytes_${cacheKey(url)}")
	}

	fun readRecord(source: String): String? {
		return readText("record_${cacheKey(source)}")
	}

	fun writeRecord(source: String, text: String) {
		writeText("record_${cacheKey(source)}", text)
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

	private fun <T> withFileLock(file_name: String, action: () -> T): T {
		val file_lock = file_locks.computeIfAbsent(file_name) { ReentrantLock() }
		file_lock.lockInterruptibly()

		try {
			return action()
		} finally {
			file_lock.unlock()

			if(!file_lock.hasQueuedThreads()) {
				file_locks.remove(file_name, file_lock)
			}
		}
	}

	private fun cacheKey(text: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
		return digest.joinToString("") { byte -> "%02x".format(byte) }
	}

	private fun cacheFile(file_name: String): Path {
		return QuantumPaths.cache_folder.resolve(file_name)
	}
}