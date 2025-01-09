package memCachedRedisRatpack

import ratpack.exec.Promise
import java.util.concurrent.CompletableFuture

fun <V> CompletableFuture<V>.toPromise(): Promise<V> =
    Promise.async { downstream ->
        this.whenComplete { result, throwable ->
            if (throwable != null) {
                downstream.error(throwable)
            } else {
                downstream.success(result)
            }
        }
    }
