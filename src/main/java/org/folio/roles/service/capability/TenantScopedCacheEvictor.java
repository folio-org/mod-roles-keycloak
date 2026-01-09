package org.folio.roles.service.capability;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

/**
 * Service for tenant-scoped cache eviction.
 *
 * <p>Evicts user permission cache entries for the current tenant without affecting other tenants.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class TenantScopedCacheEvictor {

  private static final String USER_PERMISSIONS_CACHE = "user-permissions";

  private final CacheManager cacheManager;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Evicts all user-permissions cache entries for the current tenant.
   *
   * <p>This is a best-effort operation that never throws exceptions.
   */
  public void evictUserPermissionsForCurrentTenant() {
    var tenantId = folioExecutionContext.getTenantId();

    if (tenantId == null || tenantId.isBlank()) {
      log.warn("Skipping user-permissions eviction: tenantId is blank");
      return;
    }

    try {
      var cache = cacheManager.getCache(USER_PERMISSIONS_CACHE);
      if (cache == null) {
        log.warn("Cache '{}' not found, cannot evict tenant {}", USER_PERMISSIONS_CACHE, tenantId);
        return;
      }

      if (!(cache instanceof CaffeineCache caffeineCache)) {
        log.warn("Cache '{}' is not Caffeine (type: {}), cannot evict tenant {}", USER_PERMISSIONS_CACHE,
          cache.getClass().getName(), tenantId);
        return;
      }

      evictFromNativeCache(caffeineCache.getNativeCache(), tenantId);
      log.debug("Evicted '{}' cache entries for tenant {}", USER_PERMISSIONS_CACHE, tenantId);
    } catch (Exception e) {
      log.error("Failed to evict '{}' cache for tenant {}. Cache may contain stale data until TTL expires.",
        USER_PERMISSIONS_CACHE, tenantId, e);
    }
  }

  private static void evictFromNativeCache(Cache<Object, Object> nativeCache, String tenantId) {
    var prefix = tenantId + ":";
    nativeCache.asMap().keySet().stream()
      .filter(key -> isCacheKeyForTenant(key, prefix))
      .forEach(nativeCache::invalidate);
  }

  private static boolean isCacheKeyForTenant(Object key, String tenantPrefix) {
    return key != null && key.toString().startsWith(tenantPrefix);
  }
}
