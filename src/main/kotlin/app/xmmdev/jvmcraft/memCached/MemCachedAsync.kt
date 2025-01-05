package app.xmmdev.jvmcraft.memCached

import ratpack.exec.Promise
import java.time.Instant

interface MemCachedAsync<Key : Any, Value : Any> {
    fun get(key: Key): Promise<Value>
    fun put(key: Key, value: Value, expiration: Instant? = null): Promise<Unit>
    fun putIfNotExist(key: Key, value: Value, expiration: Instant? = null): Promise<Boolean>
}
