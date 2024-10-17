package org.folio.roles.it;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.TestConstants.KC_CLIENT_ID_CACHE;
import static org.folio.roles.support.TestConstants.KC_TOKEN_CACHE;
import static org.folio.roles.support.TestConstants.KC_USER_ID_CACHE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import org.folio.roles.base.BaseIntegrationTest;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(properties = {
  "KC_CONFIG_TTL=PT0.1S",
  "KC_ADMIN_TOKEN_TTL=PT0.05S",
  "KC_CLIENT_ID_TTL=PT0.1S",
  "KC_USER_ID_CACHE_TTL=PT0.05S",
})
class CacheConfigIT extends BaseIntegrationTest {

  @Test
  void testCacheTtl() {
    var kcClientIdCache = getCaffeineCache(KC_CLIENT_ID_CACHE);
    var tokenCache = getCaffeineCache(KC_TOKEN_CACHE);
    var kcUserIdCache = getCaffeineCache(KC_USER_ID_CACHE);

    checkCacheTtl(kcClientIdCache, 100);
    checkCacheTtl(tokenCache, 50);
    checkCacheTtl(kcUserIdCache, 50);
  }

  @Test
  void testCacheEviction() {
    var kcClientIdCache = getCaffeineCache(KC_CLIENT_ID_CACHE);
    var tokenCache = getCaffeineCache(KC_TOKEN_CACHE);
    var kcUserIdCache = getCaffeineCache(KC_USER_ID_CACHE);

    checkCacheEviction(kcClientIdCache);
    checkCacheEviction(tokenCache);
    checkCacheEviction(kcUserIdCache);
  }

  @Test
  void testCacheExpiration() {
    var kcClientIdCache = getCaffeineCache(KC_CLIENT_ID_CACHE);
    var tokenCache = getCaffeineCache(KC_TOKEN_CACHE);
    var kcUserIdCache = getCaffeineCache(KC_USER_ID_CACHE);

    checkCacheExpiration(kcClientIdCache, 100);
    checkCacheExpiration(tokenCache, 50);
    checkCacheExpiration(kcUserIdCache, 50);
  }

  CaffeineCache getCaffeineCache(String name) {
    var cache = (CaffeineCache) cacheManager.getCache(name);
    assertNotNull(cache);
    return cache;
  }

  void checkCacheTtl(CaffeineCache cache, long expectedTtl) {
    cache.getNativeCache().policy().expireAfterWrite().ifPresent(ttl ->
      assertThat(ttl.getExpiresAfter().getNano()).isEqualTo(MILLISECONDS.toNanos(expectedTtl)));
  }

  void checkCacheEviction(CaffeineCache cache) {
    cache.put("key1", "value1");
    cache.put("key2", "value2");
    assertEquals(2, cache.getNativeCache().estimatedSize());
    cache.evict("key1");
    assertEquals(1, cache.getNativeCache().estimatedSize());
  }

  void checkCacheExpiration(CaffeineCache cache, long expectedTtl) {
    cache.put("key1", "value1");
    cache.put("key2", "value2");
    assertEquals(2, cache.getNativeCache().estimatedSize());
    await().atMost(ofMillis(expectedTtl + 250)).pollInterval(ofMillis(25)).untilAsserted(() -> {
      assertNull(cache.get("key1"));
      assertNull(cache.get("key2"));
    });
  }
}
