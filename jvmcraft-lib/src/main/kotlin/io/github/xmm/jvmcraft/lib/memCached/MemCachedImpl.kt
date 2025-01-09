package io.github.xmm.jvmcraft.lib.memCached

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * **`MemCachedImpl`**: An in-memory implementation of the `MemCached` interface with support for Time-To-Live (TTL).
 *
 * This class provides an in-memory caching mechanism where entries are automatically evicted
 * after their specified TTL duration. It is well-suited for applications that require a lightweight
 * cache without external dependencies.
 *
 * ### Features:
 * - In-memory caching for fast read/write operations.
 * - Configurable default TTL for cache entries.
 * - Thread-safe implementation using `ConcurrentHashMap`.
 * - Automatic expiration of entries based on their TTL.
 *
 * ### Constructor Parameters:
 * - `defaultTtl`: The default duration for which entries remain in the cache.
 *
 * ### Example Usage:
 * ```kotlin
 * val defaultTtl = Duration.ofMinutes(5)
 * val cache = MemCachedImpl<String, String>(defaultTtl)
 *
 * cache.put("key", "value")
 * val value = cache.get("key")
 * println("Retrieved value: $value") // Outputs: Retrieved value: value
 *
 * // Entry will expire after the TTL duration
 * ```
 *
 * @param Key The type of keys used in this cache.
 * @param Value The type of values stored in this cache.
 * @property defaultTtl The default time-to-live duration for cache entries.
 */
class MemCachedImpl<Key : Any, Value : Any>(
  private val defaultTtl: Duration,
) : MemCached<Key, Value> {

  /**
   * Represents a single cache entry containing a value and its expiration time.
   *
   * @param V The type of the cached value.
   * @property exp The expiration time of the cache entry.
   * @property value The value stored in the cache entry.
   */
  data class CacheEntry<V>(internal val exp: Instant, internal val value: V)

  private val storage = ConcurrentHashMap<Key, CacheEntry<Value>>()

  /**
   * Retrieves the cached value associated with the given key, if it exists and has not expired.
   *
   * @param key The key to retrieve the value for.
   * @return The cached value, or `null` if the key does not exist or the value has expired.
   */
  override fun get(key: Key): Value? =
    storage.computeIfPresent(key) { _, oldValue ->
      if (oldValue.isExpired()) null else oldValue
    }?.value

  /**
   * Adds or updates a key-value pair in the cache with an optional expiration time.
   *
   * @param key The key to add or update.
   * @param value The value to associate with the key.
   * @param expiration Optional expiration time. If `null`, the default TTL is applied.
   */
  override fun put(key: Key, value: Value, expiration: Instant?) {
    storage.put(key, CacheEntry(expiration ?: defaultExpiration(), value))
    expireEntries()
  }

  /**
   * Adds a key-value pair to the cache only if the key does not already exist.
   *
   * @param key The key to add.
   * @param value The value to associate with the key.
   * @param expiration Optional expiration time. If `null`, the default TTL is applied.
   * @return `true` if the key-value pair was added, `false` if the key already exists.
   */
  override fun putIfNotExist(key: Key, value: Value, expiration: Instant?): Boolean {
    val oldValue = storage.putIfAbsent(key, CacheEntry(expiration ?: defaultExpiration(), value))
    expireEntries()
    return oldValue == null
  }

  /**
   * Checks if the cache is empty.
   *
   * @return `true` if the cache is empty, `false` otherwise.
   */
  override fun isEmpty(): Boolean =
    storage.isEmpty()

  /**
   * Returns the number of entries currently in the cache.
   *
   * @return The size of the cache.
   */
  override fun size(): Int = storage.size

  private fun defaultExpiration(): Instant = Instant.now().plus(defaultTtl)

  private fun CacheEntry<Value>.isExpired(): Boolean {
    return Instant.now().isAfter(exp)
  }

  // We can reduce frequency of calling this method by keeping a last execution time and
  // compare it with a some timeout parameter.
  private fun expireEntries() {
    val now = Instant.now()
    storage.entries.removeIf { entry -> now.isAfter(entry.value.exp) }
  }

}