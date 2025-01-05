package app.xmmdev.jvmcraft.memCached

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.lettuce.core.codec.RedisCodec
import java.nio.ByteBuffer
import java.time.Instant

data class CacheEntry<V>(
  val exp: Instant,
  val value: V
) {
  fun isExpired(): Boolean {
    return Instant.now().isAfter(exp)
  }
}

class CacheEntryCodec<K, V> (
  private val objectMapper: ObjectMapper,
) : RedisCodec<K, CacheEntry<V>> {

  override fun decodeKey(bytes: ByteBuffer): K {
    val array = ByteArray(bytes.remaining())
    bytes.get(array)
    val typeRef = object : TypeReference<K>() {}
    return objectMapper.readValue(array, typeRef)
  }

  override fun decodeValue(bytes: ByteBuffer): CacheEntry<V> {
    val array = ByteArray(bytes.remaining())
    bytes.get(array)

    val typeRef = object : TypeReference<CacheEntry<V>>() {}
    return objectMapper.readValue(array, typeRef)
  }

  override fun encodeKey(key: K): ByteBuffer {
    val json = objectMapper.writeValueAsBytes(key)
    return ByteBuffer.wrap(json)
  }

  override fun encodeValue(value: CacheEntry<V>): ByteBuffer {
    val json = objectMapper.writeValueAsBytes(value)
    return ByteBuffer.wrap(json)
  }
}