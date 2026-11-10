package quantum.shared

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Inflater

object RemoteZip {
	private const val end_record_signature = 0x06054b50
	private const val directory_entry_signature = 0x02014b50
	private const val end_record_size = 22
	private const val directory_entry_size = 46
	private const val local_header_size = 30
	private const val max_comment_size = 65535
	private const val zip64_marker = 0xFFFFFFFFL

	fun readEntry(url: String, entry_name: String): ByteArray? {
		val tail = try {
			QuantumHttp.fetchBytes(url, "bytes=-${end_record_size + max_comment_size}")
		} catch(error: HttpStatusException) {
			if(error.status_code == 404) {
				return null
			}
			throw error
		}

		val remote_file = RemoteFile(url, tail)
		val tail_buffer = ByteBuffer.wrap(tail.body).order(ByteOrder.LITTLE_ENDIAN)
		val end_record_index = findEndRecord(tail_buffer, tail.body.size) ?: throw IOException("`$url` is not a valid zip file.")

		val directory_size = Integer.toUnsignedLong(tail_buffer.getInt(end_record_index + 12))
		val directory_offset = Integer.toUnsignedLong(tail_buffer.getInt(end_record_index + 16))

		if(directory_size == zip64_marker || directory_offset == zip64_marker) {
			throw IOException("`$url` uses the ZIP64 format, which installers never use.")
		}

		val directory_bytes = remote_file.read(directory_offset, directory_size.toInt())
		val entry = findDirectoryEntry(directory_bytes, entry_name) ?: return null

		val local_header = ByteBuffer.wrap(remote_file.read(entry.local_header_offset, local_header_size)).order(ByteOrder.LITTLE_ENDIAN)
		val name_length = java.lang.Short.toUnsignedInt(local_header.getShort(26))
		val extra_length = java.lang.Short.toUnsignedInt(local_header.getShort(28))
		val data_start = entry.local_header_offset + local_header_size + name_length + extra_length

		val compressed_bytes = remote_file.read(data_start, entry.compressed_size.toInt())
		return decompress(compressed_bytes, entry, url)
	}

	private class DirectoryEntry(
		val compression_method: Int,
		val compressed_size: Long,
		val uncompressed_size: Long,
		val local_header_offset: Long
	)

	private class RemoteFile(private val url: String, private val tail: HttpBytes) {
		private val total_size: Long = if(tail.status_code == 200) {
			tail.body.size.toLong()
		} else {
			tail.total_size ?: throw IOException("`$url` did not report its size.")
		}

		private val tail_start: Long = total_size - tail.body.size

		fun read(start: Long, length: Int): ByteArray {
			if(length == 0) {
				return ByteArray(0)
			}

			if(start >= tail_start) {
				val offset = (start - tail_start).toInt()
				return tail.body.copyOfRange(offset, offset + length)
			}

			return QuantumHttp.fetchBytes(url, "bytes=$start-${start + length - 1}").body
		}
	}

	private fun findEndRecord(buffer: ByteBuffer, size: Int): Int? {
		return (size - end_record_size downTo 0).firstOrNull { index -> buffer.getInt(index) == end_record_signature }
	}

	private fun findDirectoryEntry(directory_bytes: ByteArray, entry_name: String): DirectoryEntry? {
		val buffer = ByteBuffer.wrap(directory_bytes).order(ByteOrder.LITTLE_ENDIAN)
		var position = 0

		while(position + directory_entry_size <= directory_bytes.size && buffer.getInt(position) == directory_entry_signature) {
			val name_length = java.lang.Short.toUnsignedInt(buffer.getShort(position + 28))
			val extra_length = java.lang.Short.toUnsignedInt(buffer.getShort(position + 30))
			val comment_length = java.lang.Short.toUnsignedInt(buffer.getShort(position + 32))
			val name = String(directory_bytes, position + directory_entry_size, name_length, Charsets.UTF_8)

			if(name == entry_name) {
				return DirectoryEntry(
					compression_method = java.lang.Short.toUnsignedInt(buffer.getShort(position + 10)),
					compressed_size = Integer.toUnsignedLong(buffer.getInt(position + 20)),
					uncompressed_size = Integer.toUnsignedLong(buffer.getInt(position + 24)),
					local_header_offset = Integer.toUnsignedLong(buffer.getInt(position + 42))
				)
			}

			position += directory_entry_size + name_length + extra_length + comment_length
		}

		return null
	}

	private fun decompress(compressed_bytes: ByteArray, entry: DirectoryEntry, url: String): ByteArray {
		if(entry.compression_method == 0) {
			return compressed_bytes
		}

		if(entry.compression_method != 8) {
			throw IOException("`$url` uses an unsupported compression method `${entry.compression_method}`.")
		}

		val inflater = Inflater(true)
		val output = ByteArray(entry.uncompressed_size.toInt())
		var written = 0

		try {
			inflater.setInput(compressed_bytes + byteArrayOf(0))

			while(written < output.size && !inflater.finished()) {
				val count = inflater.inflate(output, written, output.size - written)
				if(count == 0 && inflater.needsInput()) {
					throw IOException("`$url` ended before the file was fully read.")
				}
				written += count
			}
		} finally {
			inflater.end()
		}

		return output
	}
}