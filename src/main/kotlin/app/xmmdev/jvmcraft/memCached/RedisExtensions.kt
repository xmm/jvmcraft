package app.xmmdev.jvmcraft.memCached

import io.lettuce.core.RedisFuture
import ratpack.exec.Promise

fun <V> RedisFuture<V>.toPromise(): Promise<V> =
  Promise.async { downstream ->
    this.toCompletableFuture().whenComplete { result, throwable ->
      if (throwable != null) {
        downstream.error(throwable)
      } else {
        downstream.success(result)
      }
    }
  }
