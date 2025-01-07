package io.github.xmm.jvmcraft.memCached

import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.RedisClient
import io.lettuce.core.SetArgs
import ratpack.exec.Promise
import java.time.Duration
import java.time.Instant

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
     * @return a Promise resolving to the cached value, or `null` if the value is expired or not present.
     */
    override fun get(key: K): Promise<V> {
        return redis.get(key)
            .toPromise()
            .map { if (it?.isExpired() == false) it.value else null }
    }

    /**
     * Stores the given value in the cache with the specified key and expiration time.
     *
     * If an expiration time is not provided, the default TTL is used.
     *
     * @param key the key under which the value should be stored.
     * @param value the value to be stored in the cache.
     * @param expiration the optional expiration time for the value. If `null`, the default expiration is used.
     * @return a Promise resolving to `Unit` when the operation is complete.
     */
    override fun put(key: K, value: V, expiration: Instant?): Promise<Unit> {
        return redis.set(
            key,
            CacheEntry(expiration ?: defaultExpiration(), value),
            SetArgs().pxAt(expiration ?: defaultExpiration())
        )
            .toPromise()
            .map { }
    }

    /**
     * Stores the given value in the cache only if no value is currently associated with the given key.
     *
     * If an expiration time is not provided, the default TTL is used.
     *
     * @param key the key under which the value should be stored.
     * @param value the value to be stored in the cache.
     * @param expiration the optional expiration time for the value. If `null`, the default expiration is used.
     * @return a Promise resolving to `true` if the value was successfully stored, or `false` if a value already exists.
     */
    override fun putIfNotExist(key: K, value: V, expiration: Instant?): Promise<Boolean> {
        return redis.set(
            key,
            CacheEntry(expiration ?: defaultExpiration(), value),
            SetArgs().nx().pxAt(expiration ?: defaultExpiration())
        )
            .toPromise()
            .map { it != null }
    }

    private fun defaultExpiration(): Instant = Instant.now().plus(defaultTtl)

}
