package vladsaif.syncedit.plugin.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// Exact implementation from Java 9 JDK
@Throws(IOException::class)
fun InputStream.transferTo(out: OutputStream): Long {
  var transferred: Long = 0
  val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
  var read: Int
  while (true) {
    read = this.read(buffer, 0, DEFAULT_BUFFER_SIZE)
    if (read < 0) break
    out.write(buffer, 0, read)
    transferred += read.toLong()
  }
  return transferred
}

fun InputStream.toByteArray(): ByteArray {
  val out = ByteArrayOutputStream()
  transferTo(out)
  return out.toByteArray()
}