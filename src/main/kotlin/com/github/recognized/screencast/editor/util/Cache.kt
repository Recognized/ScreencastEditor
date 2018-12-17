package com.github.recognized.screencast.editor.util

import com.intellij.util.containers.ContainerUtil
import gnu.trove.TObjectHashingStrategy

class Cache<T> private constructor(instance: Any, private val recalculate: () -> T) {
  private var myStorage: T? = null

  init {
    val list = CACHE_MAP[instance]
    if (list == null) {
      CACHE_MAP[instance] = mutableListOf<Cache<*>>().also { it.add(this) }
    } else {
      list.add(this)
    }
  }

  @Synchronized
  fun get(): T {
    return if (myStorage != null) myStorage!! else recalculate().also { myStorage = it }
  }

  @Synchronized
  fun resetCache() {
    myStorage = null
  }

  companion object {
    private val CACHE_MAP =
      ContainerUtil.createConcurrentWeakMap<Any, MutableList<Cache<*>>>(TObjectHashingStrategy.IDENTITY)

    fun <T> Any.cache(recalculate: () -> T): Cache<T> = Cache(this, recalculate)

    fun resetCache(holder: Any) {
      CACHE_MAP[holder]?.forEach(Cache<*>::resetCache)
    }
  }
}