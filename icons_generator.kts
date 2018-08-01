import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.file.Paths

fun BufferedWriter.writeln() {
    write("\n")
}

fun BufferedWriter.writeln(str: String) {
    write(str)
    write("\n")
}

val pathClass = Paths.get("src", "main", "java", "icons").toFile()
pathClass.mkdirs()
pathClass.toPath().resolve("ScreencastEditorIcons.java").toFile().outputStream().bufferedWriter().use { out ->
    out.writeln("package icons;\n")
    out.writeln()
    out.writeln("import com.intellij.openapi.util.IconLoader;\n")
    out.writeln()
    out.writeln("import javax.swing.*;\n")
    out.writeln()
    out.writeln("public interface ScreencastEditorIcons {\n")
    val path = Paths.get("resources", "icons").toFile()
    for (file in path.walk()) {
        if (file.isDirectory) continue
        if (file.nameWithoutExtension.endsWith("@2x")
                || file.nameWithoutExtension.endsWith("@2x_dark")
                || file.nameWithoutExtension.endsWith("_dark")) {
            continue
        }
        val name = if (file.nameWithoutExtension.endsWith("Tool"))
            file.nameWithoutExtension.substringBefore("Tool") + "_TOOL_WINDOW"
        else
            file.nameWithoutExtension
        out.writeln("""    Icon ${name.toUpperCase().replace('-', '_')} = IconLoader.getIcon("/icons/${file.name}"); """)
    }
    out.writeln("}")
}
