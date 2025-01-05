package app.xmmdev.jvmcraft.memCached

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.redis.testcontainers.RedisContainer
import io.lettuce.core.RedisClient
import org.junit.jupiter.api.*
import ratpack.test.exec.ExecHarness
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
        val result = ExecHarness.yieldSingle {
            memCachedRedis.get(UUID.randomUUID().toString())
        }
        assertTrue(result.isSuccess)
        assertNull(result.value)
    }

    @Test
    fun `put stores value and get returns it`() {
        ExecHarness.runSingle { exec ->
            val newKey = UUID.randomUUID().toString()

            memCachedRedis.put(newKey, "testValue").then {
                memCachedRedis.get(newKey).then { result ->
                    assertEquals("testValue", result)
                }
            }
        }
    }

    @Test
    fun `get returns null after expiration`() {
        val shortLived = Duration.ofMillis(500)
        val expiration = Instant.now().plus(shortLived)
        val newKey = UUID.randomUUID().toString()

        ExecHarness.runSingle { exec ->
            memCachedRedis.put(newKey, "testValue", expiration).then {
                Thread.sleep(700)
                memCachedRedis.get(newKey).then { result ->
                    assertNull(result)
                }
            }
        }
    }

    @Test
    fun `putIfNotExist does not overwrite existing key`() {
        val existingKey = UUID.randomUUID().toString()

        ExecHarness.runSingle { exec ->
            memCachedRedis.put(existingKey, "originalValue").then {
                memCachedRedis.putIfNotExist(existingKey, "newValue").then { wasSet ->
                    assertTrue(!wasSet)
                    memCachedRedis.get(existingKey).then { result ->
                        assertEquals("originalValue", result)
                    }
                }
            }
        }
    }

    @Test
    fun `putIfNotExist sets new key`() {
        val newKey = UUID.randomUUID().toString()
        ExecHarness.runSingle { exec ->
            memCachedRedis.putIfNotExist(newKey, "nxValue").then { wasSet ->
                assertTrue(wasSet)
                memCachedRedis.get(newKey).then { result ->
                    assertEquals("nxValue", result)
                }
            }
        }
    }
}