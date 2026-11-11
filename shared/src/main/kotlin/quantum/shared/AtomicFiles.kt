package quantum.shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object AtomicFiles {
	fun writeText(target_file: Path, text: String) {
		writeBytes(target_file, text.toByteArray(Charsets.UTF_8))
	}

	fun writeBytes(target_file: Path, bytes: ByteArray) {
		val temporary_file = Files.createTempFile(target_file.parent, "writing-", ".tmp")

		try {
			Files.write(temporary_file, bytes)
			Files.move(temporary_file, target_file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
		} finally {
			Files.deleteIfExists(temporary_file)
		}
	}
}