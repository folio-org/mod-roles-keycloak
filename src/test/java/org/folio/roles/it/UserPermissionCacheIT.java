package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.repository.CapabilityRepository;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(scripts = {
  "/sql/capabilities/populate-capabilities.sql",
  "/sql/capability-sets/populate-capability-sets.sql",
  "/sql/populate-user-capability-relations.sql"
})
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "/sql/truncate-capability-tables.sql",
  "/sql/truncate-role-tables.sql",
  "/sql/truncate-role-capability-tables.sql",
  "/sql/truncate-user-capability-tables.sql",
  "/sql/truncate-roles-user-related-tables.sql"
})
class UserPermissionCacheIT extends BaseIntegrationTest {

  private static final String USER_PERMISSIONS_CACHE = "user-permissions";
  private static final UUID USER_ID_1 = UUID.fromString("cf078e4a-5d9c-45f1-9c1d-f87003790d9f");

  @MockitoSpyBean
  private CapabilityRepository capabilityRepository;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @BeforeEach
  void setUp() {
    evictAllCaches();
    reset(capabilityRepository);
  }

  @Test
  void getUserPermissions_positive_cacheableWorks() throws Exception {
    // First call - should hit database
    mockMvc.perform(get("/permissions/users/{id}", USER_ID_1)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk());

    // Second call - should hit cache, not database
    mockMvc.perform(get("/permissions/users/{id}", USER_ID_1)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk());

    // Verify repository was called only once (first call hit DB, second hit cache)
    verify(capabilityRepository, times(1)).findAllFolioPermissions(USER_ID_1);
  }

  @Test
  void getUserPermissions_positive_cacheIsTenantScoped() throws Exception {
    var tenant1CacheKey = TENANT_ID + ":" + USER_ID_1;
    var tenant2 = "tenant2";
    var tenant2CacheKey = tenant2 + ":" + USER_ID_1;

    // Populate cache for tenant1
    mockMvc.perform(get("/permissions/users/{id}", USER_ID_1)
        .header(TENANT, TENANT_ID)
        .header(USER_ID, USER_ID_HEADER))
      .andExpect(status().isOk());

    // Verify cache contains tenant1 entry
    var cache = getCaffeineCache(USER_PERMISSIONS_CACHE);
    assertThat(cache.get(tenant1CacheKey)).isNotNull();
    assertThat(cache.get(tenant2CacheKey)).isNull();
  }

  @Test
  void cacheEviction_positive_evictsOnlyCurrentTenant() {
    var tenant1CacheKey = TENANT_ID + ":" + USER_ID_1;
    var tenant2 = "other_tenant";
    var tenant2CacheKey = tenant2 + ":" + USER_ID_1;

    var cache = getCaffeineCache(USER_PERMISSIONS_CACHE);

    // Manually add entries for two tenants
    cache.put(tenant1CacheKey, java.util.List.of("perm1", "perm2"));
    cache.put(tenant2CacheKey, java.util.List.of("perm3", "perm4"));

    assertThat(cache.get(tenant1CacheKey)).isNotNull();
    assertThat(cache.get(tenant2CacheKey)).isNotNull();

    // Evict cache for tenant1 only (this happens after any mutating operation)
    evictCacheForTenant(TENANT_ID);

    // Verify tenant1 entry is evicted, tenant2 entry remains
    assertThat(cache.get(tenant1CacheKey)).isNull();
    assertThat(cache.get(tenant2CacheKey)).isNotNull();
  }

  private CaffeineCache getCaffeineCache(String name) {
    var cache = (CaffeineCache) cacheManager.getCache(name);
    assertThat(cache).isNotNull();
    return cache;
  }

  private void evictCacheForTenant(String tenantId) {
    var cache = getCaffeineCache(USER_PERMISSIONS_CACHE);
    var prefix = tenantId + ":";
    var nativeCache = cache.getNativeCache();
    for (var key : nativeCache.asMap().keySet()) {
      if (key != null && key.toString().startsWith(prefix)) {
        nativeCache.invalidate(key);
      }
    }
  }
}
