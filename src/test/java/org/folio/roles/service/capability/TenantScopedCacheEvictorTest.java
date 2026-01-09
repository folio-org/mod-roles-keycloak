package org.folio.roles.service.capability;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantScopedCacheEvictorTest {

  private static final String USER_PERMISSIONS_CACHE = "user-permissions";
  private static final String TENANT_1 = "tenant1";
  private static final String TENANT_2 = "tenant2";

  @InjectMocks private TenantScopedCacheEvictor evictor;

  @Mock private CacheManager cacheManager;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private CaffeineCache caffeineCache;
  @Mock private Cache<Object, Object> nativeCache;

  @Test
  void evictUserPermissionsForCurrentTenant_positive_evictsKeysWithCorrectPrefix() {
    var userId1 = "user1";
    var userId2 = "user2";
    var tenant1User1Key = TENANT_1 + ":" + userId1;
    var tenant1User2Key = TENANT_1 + ":" + userId2;
    var tenant2User1Key = TENANT_2 + ":" + userId1;

    ConcurrentMap<Object, Object> cacheEntries = new ConcurrentHashMap<>(Map.of(
      tenant1User1Key, "perms1",
      tenant1User2Key, "perms2",
      tenant2User1Key, "perms3"
    ));

    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(caffeineCache);
    when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
    when(nativeCache.asMap()).thenReturn(cacheEntries);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);

    evictor.evictUserPermissionsForCurrentTenant();

    verify(nativeCache).invalidate(tenant1User1Key);
    verify(nativeCache).invalidate(tenant1User2Key);
    verify(cacheManager).getCache(USER_PERMISSIONS_CACHE);
    verify(caffeineCache).getNativeCache();
    verify(nativeCache).asMap();
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_handlesEmptyCache() {
    var emptyCache = new ConcurrentHashMap<Object, Object>();
    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(caffeineCache);
    when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
    when(nativeCache.asMap()).thenReturn(emptyCache);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);

    evictor.evictUserPermissionsForCurrentTenant();

    verify(cacheManager).getCache(USER_PERMISSIONS_CACHE);
    verify(caffeineCache).getNativeCache();
    verify(nativeCache).asMap();
    verify(nativeCache, org.mockito.Mockito.never()).invalidate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_handlesNullTenantId() {
    evictor.evictUserPermissionsForCurrentTenant();

    verifyNoInteractions(cacheManager, caffeineCache, nativeCache);
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_handlesBlankTenantId() {
    evictor.evictUserPermissionsForCurrentTenant();

    verifyNoInteractions(cacheManager, caffeineCache, nativeCache);
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_handlesMissingCache() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);
    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(null);

    evictor.evictUserPermissionsForCurrentTenant();

    verify(cacheManager).getCache(USER_PERMISSIONS_CACHE);
    verifyNoInteractions(caffeineCache, nativeCache);
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_handlesNonCaffeineCache() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);
    var nonCaffeineCache = new org.springframework.cache.concurrent.ConcurrentMapCache(USER_PERMISSIONS_CACHE);
    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(nonCaffeineCache);

    evictor.evictUserPermissionsForCurrentTenant();

    verify(cacheManager).getCache(USER_PERMISSIONS_CACHE);
    verifyNoInteractions(caffeineCache, nativeCache);
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_evictsAllEntriesForTenant() {
    var userId1 = "user1";
    var userId2 = "user2";
    var userId3 = "user3";
    var tenant1User1Key = TENANT_1 + ":" + userId1;
    var tenant1User2Key = TENANT_1 + ":" + userId2;
    var tenant1User3Key = TENANT_1 + ":" + userId3;

    ConcurrentMap<Object, Object> cacheEntries = new ConcurrentHashMap<>(Map.of(
      tenant1User1Key, "perms1",
      tenant1User2Key, "perms2",
      tenant1User3Key, "perms3"
    ));

    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(caffeineCache);
    when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
    when(nativeCache.asMap()).thenReturn(cacheEntries);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);

    evictor.evictUserPermissionsForCurrentTenant();

    verify(nativeCache).invalidate(tenant1User1Key);
    verify(nativeCache).invalidate(tenant1User2Key);
    verify(nativeCache).invalidate(tenant1User3Key);
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_doesNotEvictOtherTenants() {
    var userId1 = "user1";
    var userId2 = "user2";
    var tenant1User1Key = TENANT_1 + ":" + userId1;
    var tenant2User1Key = TENANT_2 + ":" + userId1;
    var tenant2User2Key = TENANT_2 + ":" + userId2;

    ConcurrentMap<Object, Object> cacheEntries = new ConcurrentHashMap<>(Map.of(
      tenant1User1Key, "perms1",
      tenant2User1Key, "perms2",
      tenant2User2Key, "perms3"
    ));

    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(caffeineCache);
    when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
    when(nativeCache.asMap()).thenReturn(cacheEntries);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);

    evictor.evictUserPermissionsForCurrentTenant();

    verify(nativeCache).invalidate(tenant1User1Key);
    verify(cacheManager).getCache(USER_PERMISSIONS_CACHE);
    verify(caffeineCache).getNativeCache();
    verify(nativeCache).asMap();
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_handlesNullExceptionDuringEviction() {
    var userId1 = "user1";
    var tenant1User1Key = TENANT_1 + ":" + userId1;

    ConcurrentMap<Object, Object> cacheEntries = new ConcurrentHashMap<>(Map.of(
      tenant1User1Key, "perms1"
    ));

    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(caffeineCache);
    when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
    when(nativeCache.asMap()).thenReturn(cacheEntries);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);
    when(nativeCache.asMap().keySet()).thenThrow(new RuntimeException("Test exception"));

    evictor.evictUserPermissionsForCurrentTenant();

    verify(cacheManager).getCache(USER_PERMISSIONS_CACHE);
    verify(caffeineCache).getNativeCache();
    verify(nativeCache).asMap();
  }

  @Test
  void evictUserPermissionsForCurrentTenant_positive_handlesTenantIdWithColon() {
    var userId1 = "user1";
    var tenant1User1Key = TENANT_1 + ":" + userId1;

    ConcurrentMap<Object, Object> cacheEntries = new ConcurrentHashMap<>(Map.of(
      tenant1User1Key, "perms1"
    ));

    when(cacheManager.getCache(USER_PERMISSIONS_CACHE)).thenReturn(caffeineCache);
    when(caffeineCache.getNativeCache()).thenReturn(nativeCache);
    when(nativeCache.asMap()).thenReturn(cacheEntries);
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_1);

    evictor.evictUserPermissionsForCurrentTenant();

    verify(nativeCache).invalidate(tenant1User1Key);
  }
}
