package org.folio.roles.integration.keyclock;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.keyclock.model.KeycloakRealmConfiguration;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@UnitTest
@SpringBootTest(classes = {RealmConfigurationProvider.class,
                           RealmConfigurationProviderTest.TestContextConfiguration.class})
class RealmConfigurationProviderTest {

  private static final String CLIENT_ID = "folio-backend-admin";
  private static final String TENANT_ID = "master";
  private static final String CACHE_NAME = "keycloak-configuration";
  private static final String SECRET = "kc-client-secret";
  private static final String KEY = String.format("%s_%s_%s", "test", TENANT_ID, CLIENT_ID);

  @Autowired
  private RealmConfigurationProvider realmConfigurationProvider;
  @Autowired
  private CacheManager cacheManager;
  @MockitoBean
  private SecureStore secureStore;
  @MockitoBean
  private KeycloakConfigurationProperties keycloakConfigurationProperties;
  @MockitoBean private SecureStoreProperties secureStoreProperties;

  @AfterEach
  void tearDown() {
    cacheManager.getCacheNames().forEach(cacheName -> requireNonNull(cacheManager.getCache(cacheName)).clear());
  }

  @Test
  void getRealmConfiguration_positive() throws Exception {
    when(keycloakConfigurationProperties.getClientId()).thenReturn(CLIENT_ID);
    when(secureStoreProperties.getEnvironment()).thenReturn("test");
    when(secureStore.get(KEY)).thenReturn(SECRET);

    var actual = realmConfigurationProvider.getRealmConfiguration();

    var expectedValue = new KeycloakRealmConfiguration()
      .clientId(CLIENT_ID)
      .clientSecret(SECRET);

    assertThat(actual).isEqualTo(expectedValue);
    assertThat(getCachedValue()).isPresent().get().isEqualTo(expectedValue);
  }

  @Test
  void getRealmConfiguration_clientSecretNotFound() throws Exception {
    when(keycloakConfigurationProperties.getClientId()).thenReturn(CLIENT_ID);
    when(secureStoreProperties.getEnvironment()).thenReturn("test");
    when(secureStore.get(KEY)).thenThrow(new SecretNotFoundException("not found"));

    assertThatThrownBy(() -> realmConfigurationProvider.getRealmConfiguration())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to get value from secure store [clientId: " + CLIENT_ID + "]");

    assertThat(getCachedValue()).isEmpty();
  }

  private Optional<Object> getCachedValue() {
    return ofNullable(cacheManager.getCache(CACHE_NAME))
      .map(cache -> cache.get("keycloak-config"))
      .map(ValueWrapper::get);
  }

  @EnableCaching
  @TestConfiguration
  static class TestContextConfiguration {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager(CACHE_NAME);
    }

    @Bean
    FolioExecutionContext folioExecutionContext() {
      return new DefaultFolioExecutionContext(null, Map.of(TENANT, singletonList(TENANT_ID)));
    }
  }
}
