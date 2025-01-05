package app.xmmdev.jvmcraft.memCached

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MemCachedImplTest {

    private lateinit var memCached: MemCachedImpl<String, String>
    private val defaultTtl = Duration.ofSeconds(1)

    @BeforeAll
    fun setup() {
        memCached = MemCachedImpl(defaultTtl)
    }

    @AfterEach
    fun tearDown() {
        memCached = MemCachedImpl(defaultTtl)
    }

    @Test
    fun `put and get should store and retrieve value`() {
        val key = "testKey"
        val value = "testValue"

        memCached.put(key, value)
        val retrieved = memCached.get(key)

        assertEquals(value, retrieved, "Retrieved value should match the stored value")
    }

    @Test
    fun `get should return null for non-existent key`() {
        val retrieved = memCached.get("nonExistentKey")
        assertNull(retrieved, "Retrieved value should be null for a non-existent key")
    }

    @Test
    fun `get should return null after entry has expired`() {
        val key = "tempKey"
        val value = "tempValue"

        memCached.put(key, value)
        // Wait for TTL to expire
        Thread.sleep(defaultTtl.toMillis() + 100)

        val retrieved = memCached.get(key)
        assertNull(retrieved, "Retrieved value should be null after expiration")
    }

    @Test
    fun `putIfNotExist should store value if key does not exist`() {
        val key = "uniqueKey"
        val value = "uniqueValue"

        val result = memCached.putIfNotExist(key, value, null)
        val retrieved = memCached.get(key)

        assertTrue(result, "putIfNotExist should return true when key does not exist")
        assertEquals(value, retrieved, "Retrieved value should match the stored value")
    }

    @Test
    fun `putIfNotExist should not overwrite existing key`() {
        val key = "existingKey"
        val initialValue = "initialValue"
        val newValue = "newValue"

        memCached.put(key, initialValue)
        val result = memCached.putIfNotExist(key, newValue, null)
        val retrieved = memCached.get(key)

        assertFalse(result, "putIfNotExist should return false when key already exists")
        assertEquals(initialValue, retrieved, "Existing value should not be overwritten")
    }

    @Test
    fun `isEmpty should return true for empty cache`() {
        assertTrue(memCached.isEmpty(), "Cache should be empty initially")
    }

    @Test
    fun `isEmpty should return false after adding an entry`() {
        memCached.put("key", "value")
        assertFalse(memCached.isEmpty(), "Cache should not be empty after adding an entry")
    }

    @Test
    fun `isEmpty should return true after all entries have expired`() {
        memCached.put("key1", "value1")
        memCached.put("key2", "value2")

        // Wait for TTL to expire
        Thread.sleep(defaultTtl.toMillis() + 100)

        // Accessing to trigger expiration
        memCached.get("key1")
        memCached.get("key2")

        assertTrue(
            memCached.isEmpty(),
            "Cache should be empty after all entries have expired"
        )
    }

    @Test
    fun `put should update the value for an existing key`() {
        val key = "updateKey"
        val initialValue = "initial"
        val updatedValue = "updated"

        memCached.put(key, initialValue)
        var retrieved = memCached.get(key)
        assertEquals(initialValue, retrieved, "Initial value should be stored correctly")

        memCached.put(key, updatedValue)
        retrieved = memCached.get(key)
        assertEquals(updatedValue, retrieved, "Value should be updated correctly")
    }

    @Test
    fun `putIfNotExist should respect custom expiration`() {
        val key = "customExpiryKey"
        val value = "value"
        val customExpiry = Instant.now().plusMillis(500) // 0.5 seconds

        val result = memCached.putIfNotExist(key, value, customExpiry)
        assertTrue(result, "putIfNotExist should return true when key does not exist")

        val retrieved = memCached.get(key)
        assertEquals(value, retrieved, "Retrieved value should match the stored value")

        // Wait for custom expiry
        Thread.sleep(600)

        val expired = memCached.get(key)
        assertNull(expired, "Retrieved value should be null after custom expiration")
    }

    @Test
    fun `expireEntries should remove only expired entries`() {
        memCached.put("key1", "value1") // Expires in 1 second
        memCached.put("key2", "value2") // Expires in 1 second

        // Wait for half the TTL
        Thread.sleep(500)

        // Put a new key with a longer TTL
        memCached.put("key3", "value3")

        // Wait for another 600ms (total 1.1 seconds)
        Thread.sleep(600)

        // Access to trigger expiration
        memCached.get("key1")
        memCached.get("key2")
        memCached.get("key3")

        assertNull(memCached.get("key1"), "key1 should have expired")
        assertNull(memCached.get("key2"), "key2 should have expired")
        assertEquals("value3", memCached.get("key3"), "key3 should still be valid")
    }
}