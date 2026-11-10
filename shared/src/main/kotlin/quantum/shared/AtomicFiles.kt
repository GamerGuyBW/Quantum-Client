package quantum.shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object AtomicFiles {
	fun writeText(target_file: Path, text: String) {
		val temporary_file = Files.createTempFile(target_file.parent, "writing-", ".tmp")

		try {
			Files.writeString(temporary_file, text)
			Files.move(temporary_file, target_file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
		} finally {
			Files.deleteIfExists(temporary_file)
		}
	}
}