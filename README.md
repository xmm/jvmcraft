# Extension for JVM Libraries

A library of tools and extensions for Java and Kotlin designed to simplify working with various third-party libraries. It now includes support for reactive programming through the introduction of Reactor-based utilities and maintains robust tools for in-memory and distributed caching.
## Features

### **Caching Utilities**
- **`MemCache`**: An interface that defines a simple in-memory, synchronous cache designed for single-instance applications.
- **`MemCachedImpl`**: An implementation of the MemCached interface that uses a `ConcurrentHashMap`, making it thread-safe and well-suited for concurrent environments.
- **`MemCachedAsync`**: An interface that defines an asynchronous cache for storing and retrieving key-value pairs. This interface is designed for use with Java's `CompletableFuture` for seamless integration with asynchronous programming models.
- **`MemCachedRedis`**: An implementation of the `MemCachedAsync` interface that uses Redis as the underlying storage, leveraging the Lettuce package. This implementation is well-suited for multi-instance applications, as it uses Redis for distributed caching.

### **Reactive Utilities**
- **`PagingPublisher`**: A reactive publisher designed for efficient asynchronous paging. It supports backpressure and integrates seamlessly with Reactor's `Flux` and reactive-streams.
    - Dynamically fetches pages of data based on an initial value and a generator function.
    - Compatible with `reactor-core` and adheres to the reactive-streams specification.

## Installation

Add the library to your project by including it in your `build.gradle` or `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.xmm:jvmcraft-lib:<VERSION>")
}
```

## Usage Examples

### Using `MemCache`

```kotlin
val memCache: MemCached<String, String> = MemCachedImpl(Duration.ofMinutes(5))
val isSaved = memCache.putIfNotExist("key", "value", ttl = 300)
assertTrue(isSaved)

val retrieved: String? = memCache.get("key")
assertEquals("value", retrieved)
```

### Using `MemCacheAsync`

```kotlin
val redisClient = RedisClient.create("redis://localhost:6379")
val objectMapper = ObjectMapper()
    .registerKotlinModule()
    .registerModule(Jdk8Module())
    .registerModule(JavaTimeModule())
val defaultTtl = Duration.ofMinutes(10)
val cache = MemCachedRedis<String, String>(redisClient, objectMapper, defaultTtl)

cache.put("key", "value").thenAccept {
    println("Value cached successfully!")
}

cache.get("key").thenAccept { value ->
    println("Retrieved value: $value")
}
```

### Using `PagingPublisher`

```java
import reactor.core.publisher.Flux;
import io.github.xmm.jvmcraft.reactor.PagingPublisher;

class Filter {
  public int max;
  public Long lastId;
}

PagingPublisher.create(
      () -> null,
      (lastRecord) -> someRepository.findByFilters(
          filter.withLastId(lastRecord != null ? lastRecord.id() : null))
);
publisher.subscribe(System.out::println);
```

## License

This project is licensed under the MIT License. See the LICENSE.txt file for details.

