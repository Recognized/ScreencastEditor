package vladsaif.syncedit.plugin.recognition

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import kotlin.coroutines.experimental.Continuation

suspend fun <T> ApiFuture<T>.await(): T {
    try {
        if (isDone) return get() as T
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }

    return suspendCancellableCoroutine { cont: CancellableContinuation<T> ->
        val callback = ContinuationCallback(cont)
        ApiFutures.addCallback(this, callback, MoreExecutors.directExecutor())
        cont.invokeOnCancellation {
            callback.cont = null
        }
    }
}

private class ContinuationCallback<T>(
        @Volatile @JvmField var cont: Continuation<T>?
) : ApiFutureCallback<T> {
    @Suppress("UNCHECKED_CAST")
    override fun onSuccess(result: T?) {
        cont?.resume(result as T)
    }

    override fun onFailure(t: Throwable) {
        cont?.resumeWithException(t)
    }
}

