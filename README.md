# Kotlin Utility Library

A library of tools and extensions for Java and Kotlin designed to simplify working with various third-party libraries. Currently, it includes two tools for interacting with Memcached.

## Features

- **`MemCache`**: An interface that defines a simple in-memory, synchronous cache designed for single-instance applications.
- **`MemCachedImpl`**: An implementation of the MemCached interface that uses a ConcurrentHashMap, making it thread-safe and well-suited for concurrent environments.
- **`MemCacheAsync`**: An interface that defines an asynchronous cache designed for use with the Ratpack framework. It returns a ratpack.Promise, enabling seamless integration with Ratpack’s asynchronous programming model.
- **`MemCacheRedis`**: An implementation of the MemCacheAsync interface that utilizes Redis as the underlying storage through the Lettuce package. This implementation is ideal for multi-instance applications, as it leverages Redis to provide distributed caching.

The primary goal of both MemCached and MemCacheAsync is to atomically check for the presence of a key and save data only if the key is unique, ensuring consistent and reliable caching behavior.

## Installation

Add the library to your project by including it in your `build.gradle` or `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.xmm:jvmcraft:<VERSION>")
}
```

## Usage Examples

### Using MemCache

```kotlin
val memCache: MemCached<String, String> = MemCachedImpl(Duration.ofMinutes(5))
val isSaved = memCache.putIfNotExist("key", "value", ttl = 300)
assertTrue(isSaved)

val retrieved: String? = memCache.get("key")
assertEqual("value", retrieved)
```

### Using MemCacheAsync

```kotlin
val redisClient = RedisClient.create("redis://localhost")
val mapper: ObjectMapper = jacksonObjectMapper()
val memCacheAsync: MemCachedAsync<String, String> = MemCachedRedis(redisClient, mapper, Duration.ofMinutes(5))
ExecHarness.runSingle { exec ->
    memCacheAsync.putIfNotExist("newKey", "value").then { wasSet ->
        assertTrue(wasSet)
        memCacheAsync.get("newKey").then { result ->
            assertEquals("value", result)
        }
    }
}
```

## License

This project is licensed under the MIT License. See the LICENSE file for details.
