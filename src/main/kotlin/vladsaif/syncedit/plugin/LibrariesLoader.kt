package vladsaif.syncedit.plugin

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.util.lang.UrlClassLoader
import java.io.File
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

class LibrariesLoader : ApplicationComponent {
  companion object {
    private val myUrlClassLoader: ClassLoader

    init {
      val loadedUrls = (LibrariesLoader::class.java.classLoader as PluginClassLoader).urls
      val path = loadedUrls.first()!!.toString().substringBefore("lib/")
      val extUrl = URL(path + "ext")
      val urls = File(extUrl.toURI()).walk().map { it.toURI().toURL() }.toMutableList()
      urls.removeAt(0)
      myUrlClassLoader = UrlClassLoader.build().urls(urls).parent(ClassLoader.getSystemClassLoader()).get()
    }

    fun getGSpeechKit(): Class<*> {
      return Class.forName("vladsaif.gspeech.GSpeechKit", true, myUrlClassLoader)!!
    }

    fun createGSpeechKitInstance(path: Path): Any {
      Files.newInputStream(path).use {
        try {
          return getGSpeechKit().getConstructor(InputStream::class.java).newInstance(it)
        } catch (ex: InvocationTargetException) {
          throw ex.targetException
        }
      }
    }

    fun checkCredentials(path: Path) {
      Files.newInputStream(path).use {
        try {
          getGSpeechKit().getConstructor(InputStream::class.java).newInstance(it)
        } catch (ex: InvocationTargetException) {
          throw  ex.targetException
        }
      }
    }
  }

}