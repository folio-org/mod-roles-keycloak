package org.folio.roles.base;

import static java.util.Objects.requireNonNull;
import static org.folio.roles.support.TestConstants.MODULE_NAME;
import static org.folio.roles.support.TestConstants.OKAPI_AUTH_TOKEN;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.test.TestUtils.asJsonString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.test.base.BaseBackendIntegrationTest;
import org.folio.test.extensions.EnableKafka;
import org.folio.test.extensions.EnableKeycloak;
import org.folio.test.extensions.EnablePostgres;
import org.folio.test.extensions.EnableWireMock;
import org.folio.test.extensions.impl.KeycloakExecutionListener;
import org.folio.test.extensions.impl.WireMockAdminClient;
import org.folio.test.extensions.impl.WireMockExecutionListener;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Log4j2
@EnableKafka
@EnableWireMock
@EnablePostgres
@SpringBootTest
@EnableKeycloak
@ActiveProfiles("it")
@AutoConfigureMockMvc
@DirtiesContext(classMode = AFTER_CLASS)
@Import(BaseIntegrationTest.TopicConfiguration.class)
@TestExecutionListeners(listeners = {WireMockExecutionListener.class, KeycloakExecutionListener.class},
  mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public abstract class BaseIntegrationTest extends BaseBackendIntegrationTest {

  protected static final String FOLIO_IT_CAPABILITIES_TOPIC = "it-test.test.mgr-tenant-entitlements.capability";
  protected static WireMockAdminClient wmAdminClient;

  @Autowired protected CacheManager cacheManager;

  @AfterEach
  void afterEach() {
    evictAllCaches();
  }

  public void evictAllCaches() {
    for (var cacheName : cacheManager.getCacheNames()) {
      requireNonNull(cacheManager.getCache(cacheName)).clear();
    }
  }

  public static ResultActions attemptGet(String uri, Object... args) throws Exception {
    return mockMvc.perform(get(uri, args).contentType(APPLICATION_JSON).headers(okapiHeaders()));
  }

  protected static ResultActions attemptPost(String uri, Object body, Object... args) throws Exception {
    return mockMvc.perform(post(uri, args)
      .headers(okapiHeaders())
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptPut(String uri, Object body, Object... args) throws Exception {
    return mockMvc.perform(put(uri, args)
      .headers(okapiHeaders())
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptDelete(String uri, Object... args) throws Exception {
    return mockMvc.perform(delete(uri, args)
      .headers(okapiHeaders())
      .contentType(APPLICATION_JSON));
  }

  public static ResultActions doGet(String uri, Object... args) throws Exception {
    return attemptGet(uri, args).andExpect(status().isOk());
  }

  public static ResultActions doGet(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request.contentType(APPLICATION_JSON)).andExpect(status().isOk());
  }

  protected static ResultActions doPost(String uri, Object body, Object... args) throws Exception {
    return attemptPost(uri, body, args).andExpect(status().isCreated());
  }

  protected static ResultActions doPut(String uri, Object body, Object... args) throws Exception {
    return attemptPut(uri, body, args).andExpect(status().isOk());
  }

  protected static ResultActions doDelete(String uri, Object... args) throws Exception {
    return attemptDelete(uri, args).andExpect(status().isNoContent());
  }

  protected static HttpHeaders okapiHeaders() {
    var headers = new HttpHeaders();
    headers.add(XOkapiHeaders.URL, wmAdminClient.getWireMockUrl());
    headers.add(XOkapiHeaders.TOKEN, OKAPI_AUTH_TOKEN);
    headers.add(XOkapiHeaders.TENANT, TENANT_ID);
    return headers;
  }

  @SneakyThrows
  protected static void enableTenant(String tenant) {
    enableTenant(tenant, new TenantAttributes());
  }

  @SneakyThrows
  protected static void enableTenant(String tenant, TenantAttributes tenantAttributes) {
    tenantAttributes.moduleTo(MODULE_NAME);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(XOkapiHeaders.TENANT, tenant))
      .andExpect(status().isNoContent());
  }

  @SneakyThrows
  protected static void removeTenant(String tenantId) {
    var tenantAttributes = new TenantAttributes().moduleFrom(MODULE_NAME).purge(true);
    mockMvc.perform(post("/_/tenant")
        .content(asJsonString(tenantAttributes))
        .contentType(APPLICATION_JSON)
        .header(XOkapiHeaders.TENANT, tenantId))
      .andExpect(status().isNoContent());
  }

  @TestConfiguration
  public static class TopicConfiguration {

    @Bean
    public NewTopic capabilitiesTopic() {
      return new NewTopic(FOLIO_IT_CAPABILITIES_TOPIC, 1, (short) 1);
    }
  }
}
