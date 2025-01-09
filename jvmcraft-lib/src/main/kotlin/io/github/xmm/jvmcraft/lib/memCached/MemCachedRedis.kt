package io.github.xmm.jvmcraft.lib.memCached

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

/**
 * **`MemCachedRedis`**: An implementation of the `MemCachedAsync` interface that uses Redis as the underlying storage.
 *
 * This class is ideal for applications that require distributed caching in a multi-instance setup.
 * It uses the Lettuce package to interact with Redis and provides support for storing key-value pairs
 * asynchronously with Java's `CompletableFuture`.
 *
 * ### Features:
 * - Distributed caching through Redis.
 * - Asynchronous operations using `CompletableFuture`.
 * - Configurable default Time-To-Live (TTL) for cached entries.
 * - Serialization and deserialization using a customizable codec.
 *
 * ### Constructor Parameters:
 * - `redisClient`: An instance of `RedisClient` used to connect to the Redis server.
 * - `objectMapper`: An `ObjectMapper` for serializing and deserializing keys and values.
 * - `defaultTtl`: The default duration for which entries remain in the cache.
 *
 * ### Example Usage:
 * ```kotlin
 * val redisClient = RedisClient.create("redis://localhost:6379")
 * val objectMapper = ObjectMapper()
 * val defaultTtl = Duration.ofMinutes(10)
 * val cache = MemCachedRedis<String, String>(redisClient, objectMapper, defaultTtl)
 *
 * cache.put("key", "value").thenAccept {
 *     println("Value cached successfully!")
 * }
 *
 * cache.get("key").thenAccept { value ->
 *     println("Retrieved value: $value")
 * }
 * ```
 *
 * @param K The type of keys used in the cache.
 * @param V The type of values stored in the cache.
 */
class MemCachedRedis<K : Any, V : Any>(
    redisClient: RedisClient,
    objectMapper: ObjectMapper,
    private val defaultTtl: Duration,
) : MemCachedAsync<K, V> {

    private val codec = CacheEntryCodec<K, V>(objectMapper)
    private var redis = redisClient.connect(codec).async()

    /**
     * Retrieves the value associated with the given key from the cache.
     *
     * If the cached value exists but is expired, this method will return `null`.
     *
     * @param key the key whose associated value is to be retrieved.
     * @return a [CompletableFuture] resolving to the cached value, or `null` if the value is expired or not present.
     */
    override fun get(key: K): CompletableFuture<V> {
        return redis.get(key)
            .toCompletableFuture()
            .thenApply { if (it?.isExpired() == false) it.value else null }
    }

    /**
     * Stores the given value in the cache with the specified key and expiration time.
     *
     * If an expiration time is not provided, the default TTL is used.
     *
     * @param key the key under which the value should be stored.
     * @param value the value to be stored in the cache.
     * @param expiration the optional expiration time for the value. If `null`, the default expiration is used.
     * @return a [CompletableFuture] resolving to `Unit` when the operation is complete.
     */
    override fun put(key: K, value: V, expiration: Instant?): CompletableFuture<Unit> {
        return redis.set(
            key,
            CacheEntry(expiration ?: defaultExpiration(), value),
            SetArgs().pxAt(expiration ?: defaultExpiration())
        )
            .toCompletableFuture()
            .thenApply { }
    }

    /**
     * Stores the given value in the cache only if no value is currently associated with the given key.
     *
     * If an expiration time is not provided, the default TTL is used.
     *
     * @param key the key under which the value should be stored.
     * @param value the value to be stored in the cache.
     * @param expiration the optional expiration time for the value. If `null`, the default expiration is used.
     * @return a [CompletableFuture] resolving to `true` if the value was successfully stored, or `false` if a value already exists.
     */
    override fun putIfNotExist(key: K, value: V, expiration: Instant?): CompletableFuture<Boolean> {
        return redis.set(
            key,
            CacheEntry(expiration ?: defaultExpiration(), value),
            SetArgs().nx().pxAt(expiration ?: defaultExpiration())
        )
            .toCompletableFuture()
            .thenApply { it != null }
    }

    private fun defaultExpiration(): Instant = Instant.now().plus(defaultTtl)

}
