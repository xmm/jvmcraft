package memCachedRedisRatpack

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.redis.testcontainers.RedisContainer
import io.github.xmm.jvmcraft.lib.memCached.MemCachedAsync
import io.github.xmm.jvmcraft.lib.memCached.MemCachedRedis
import io.lettuce.core.RedisClient
import ratpack.server.RatpackServer
import java.time.Duration
import kotlin.apply

fun main() {
    RatpackServer.start { server ->
        server
            .serverConfig { server ->
                server.port(5050)
            }
            .registryOf { r ->
                val objectMapper = ObjectMapper()
                    .registerKotlinModule()
                    .registerModule(Jdk8Module())
                    .registerModule(JavaTimeModule())

                val redisContainer = RedisContainer("redis")
                    .apply {
                        start()
                        Runtime.getRuntime().addShutdownHook(Thread {
                            stop()
                        })
                    }

                val redisClient = RedisClient.create(redisContainer.redisURI)

                val defaultTtl = Duration.ofSeconds(30)

                // Register MemCachedRedis as MemCachedAsync
                val memCached =
                    MemCachedRedis<String, String>(redisClient, objectMapper, defaultTtl)
                r.add(MemCachedAsync::class.java, memCached)
            }
            .handlers { chain ->
                chain
                    .get("get/:key") { ctx ->
                        val key: String? = ctx.pathTokens["key"]
                        if (key != null) {
                            @Suppress("UNCHECKED_CAST") // It can be avoided using google guice
                            val cache =
                                ctx.get(MemCachedAsync::class.java) as MemCachedAsync<String, String>
                            cache.get(key)
                                .toPromise()
                                .then { value ->
                                    ctx.render("Fetch Key: $key Value: $value")
                                }
                        } else {
                            ctx.response.status(400)
                        }
                    }

                    .post("put/:key/:value") { ctx ->
                        val key: String? = ctx.pathTokens["key"]
                        val value: String? = ctx.pathTokens["value"]
                        if (key != null && value != null) {
                            @Suppress("UNCHECKED_CAST")
                            val cache =
                                ctx.get(MemCachedAsync::class.java) as MemCachedAsync<String, String>
                            cache.put(key, value)
                                .toPromise()
                                .then {
                                    ctx.render("Store Key: $key Value: $value")
                                }
                        } else {
                            ctx.response.status(400)
                        }
                    }
            }
    }
}