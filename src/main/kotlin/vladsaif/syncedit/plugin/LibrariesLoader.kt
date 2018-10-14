package vladsaif.syncedit.plugin

import com.intellij.ide.plugins.cl.PluginClassLoader
import com.intellij.util.lang.UrlClassLoader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader

class LibrariesLoader {
  companion object {
    private val myUrls: List<URL>
    private var myLoaderField: ClassLoader? = null
    private val myUrlClassLoader: ClassLoader
      get() {
        if (myLoaderField == null) {
          myLoaderField = UrlClassLoader.build().urls(myUrls).parent(ClassLoader.getSystemClassLoader()).get()
        }
        return myLoaderField!!
      }

    init {
      // Google libraries work abnormally when loaded with default classloader
      // It finds android.app.Application class and assumes that it is running under Android. Then it fails.
      // So to prevent it from finding it, we should load these classes with system classloader as parent.
      val loadedUrls = (LibrariesLoader::class.java.classLoader as? PluginClassLoader)?.urls
          ?: (LibrariesLoader::class.java.classLoader as URLClassLoader).urLs.asList()
      myUrls = loadedUrls
    }

    fun getGSpeechKit(): Class<*> {
      return Class.forName("vladsaif.syncedit.plugin.GSpeechKitInternal", true, myUrlClassLoader)!!
    }

    fun createGSpeechKitInstance(key: String): Any {
      ByteArrayInputStream(key.toByteArray(Charsets.UTF_8)).use {
        try {
          return getGSpeechKit().getConstructor(InputStream::class.java).newInstance(it)
        } catch (ex: InvocationTargetException) {
          throw ex.targetException
        }
      }
    }

    fun checkCredentials(inputStream: InputStream) {
      inputStream.use {
        try {
          getGSpeechKit().getConstructor(InputStream::class.java).newInstance(it)
        } catch (ex: InvocationTargetException) {
          throw ex.targetException
        }
      }
    }

    fun releaseClassloader() {
      myLoaderField = null
    }
  }
}