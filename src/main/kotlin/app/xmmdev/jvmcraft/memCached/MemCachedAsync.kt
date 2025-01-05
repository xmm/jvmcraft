package app.xmmdev.jvmcraft.memCached

import ratpack.exec.Promise
import java.time.Instant

/**
 * **`MemCachedAsync`**: An interface that defines an asynchronous cache for storing and retrieving key-value pairs.
 *
 * This interface is designed for use with the Ratpack framework and leverages [Promise] for seamless integration
 * with Ratpack’s asynchronous programming model.
 *
 * Implementations of this interface, like [MemCachedRedis], should handle expiration times for cached entries,
 * with support for optional default expiration values.
 *
 * @param Key The type of keys used in the cache.
 * @param Value The type of values stored in the cache.
 */
interface MemCachedAsync<Key : Any, Value : Any> {

    /**
     * Retrieves the value associated with the given [key] from the cache.
     *
     * Implementations should handle expiration, returning `null` if the value is expired or not present in the cache.
     *
     * @param key The key whose associated value is to be retrieved.
     * @return A [Promise] resolving to the cached value, or `null` if the value is not present or has expired.
     */
    fun get(key: Key): Promise<Value>

    /**
     * Stores the given [value] in the cache under the specified [key], with an optional expiration time.
     *
     * If no expiration time is provided, implementations should use a default TTL (time-to-live) value.
     *
     * @param key The key under which the value should be stored.
     * @param value The value to be stored in the cache.
     * @param expiration Optional expiration time for the cache entry. If `null`, the default expiration time is used.
     * @return A [Promise] resolving to `Unit` when the operation is complete.
     */
    fun put(key: Key, value: Value, expiration: Instant? = null): Promise<Unit>

    /**
     * Stores the given [value] in the cache only if no value is currently associated with the given [key].
     *
     * If no expiration time is provided, implementations should use a default TTL (time-to-live) value.
     *
     * @param key The key under which the value should be stored.
     * @param value The value to be stored in the cache.
     * @param expiration Optional expiration time for the cache entry. If `null`, the default expiration time is used.
     * @return A [Promise] resolving to `true` if the value was successfully stored, or `false` if a value already exists for the key.
     */
    fun putIfNotExist(key: Key, value: Value, expiration: Instant? = null): Promise<Boolean>
}