package io.github.xmm.jvmcraft.lib.memCached

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.redis.testcontainers.RedisContainer
import io.lettuce.core.RedisClient
import org.junit.jupiter.api.*
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemCachedRedisTest {

    private lateinit var objectMapper: ObjectMapper
    private lateinit var redisContainer: RedisContainer
    private lateinit var redisClient: RedisClient
    private lateinit var memCachedRedis: MemCachedRedis<String, String>

    @BeforeAll
    fun setupSpec() {
        redisContainer = RedisContainer("redis")
            .apply {
                start()
                Runtime.getRuntime().addShutdownHook(Thread {
                    stop()
                })
            }
        redisClient = RedisClient.create(redisContainer.redisURI)

        objectMapper = jacksonObjectMapper()
            .registerKotlinModule()
            .registerModule(Jdk8Module())
            .registerModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @AfterAll
    fun cleanupSpec() {
        redisClient.close()
    }

    @BeforeEach
    fun setUp() {
        memCachedRedis = MemCachedRedis<String, String>(
            redisClient,
            objectMapper,
            Duration.ofSeconds(5),
        )
    }

    @Test
    fun `get returns null when key is not present`() {
        val key = UUID.randomUUID().toString()
        val result = memCachedRedis.get(key).get()
        assertNull(result)
    }

    @Test
    fun `put stores value and get returns it`() {
        val newKey = UUID.randomUUID().toString()
        memCachedRedis.put(newKey, "testValue").join()

        val result = memCachedRedis.get(newKey).get()
        assertEquals("testValue", result)
    }

    @Test
    fun `get returns null after expiration`() {
        val shortLived = Duration.ofMillis(500)
        val expiration = Instant.now().plus(shortLived)
        val newKey = UUID.randomUUID().toString()

        memCachedRedis.put(newKey, "testValue", expiration).join()
        Thread.sleep(700)
        val result = memCachedRedis.get(newKey).get()
        assertNull(result)
    }

    @Test
    fun `putIfNotExist does not overwrite existing key`() {
        val existingKey = UUID.randomUUID().toString()

        memCachedRedis.put(existingKey, "originalValue").join()
        val wasSet = memCachedRedis.putIfNotExist(existingKey, "newValue").join()
        assertTrue(!wasSet)

        val result = memCachedRedis.get(existingKey).get()
        assertEquals("originalValue", result)
    }

    @Test
    fun `putIfNotExist sets new key`() {
        val newKey = UUID.randomUUID().toString()

        val wasSet = memCachedRedis.putIfNotExist(newKey, "nxValue").join()
        assertTrue(wasSet)

        val result = memCachedRedis.get(newKey).get()
        assertEquals("nxValue", result)
    }
}