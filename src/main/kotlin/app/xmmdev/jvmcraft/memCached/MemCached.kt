package app.xmmdev.jvmcraft.memCached

import java.time.Instant

/**
 * Interface for a simple in-memory key-value cache with optional expiration.
 *
 * @param Key The type of keys used in this cache.
 * @param Value The type of values stored in this cache.
 */
interface MemCached<Key : Any, Value : Any> {

    /**
     * Retrieves the value associated with the given [key], if present and not expired.
     *
     * @param key The key whose associated value is to be retrieved.
     * @return The cached value, or `null` if the key is not present or the value has expired.
     */
    fun get(key: Key): Value?

    /**
     * Stores the [key]-[value] pair in the cache with an optional expiration time.
     *
     * @param key The key to store the value under.
     * @param value The value to be stored.
     * @param expiration The expiration time for the entry. If `null`, the default TTL will be applied.
     */
    fun put(key: Key, value: Value, expiration: Instant? = null)

    /**
     * Stores the [key]-[value] pair in the cache only if the key does not already exist.
     *
     * @param key The key to store the value under.
     * @param value The value to be stored.
     * @param expiration The expiration time for the entry. If `null`, the default TTL will be applied.
     * @return `true` if the key was successfully added, or `false` if the key already exists.
     */
    fun putIfNotExist(key: Key, value: Value, expiration: Instant? = null): Boolean

    /**
     * Checks if the cache is empty.
     *
     * @return `true` if the cache is empty, or `false` otherwise.
     */
    fun isEmpty(): Boolean

    /**
     * Returns the number of entries currently stored in the cache.
     *
     * @return The size of the cache.
     */
    fun size(): Int
}