package org.folio.roles.it;

import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestUtils.await;
import static org.folio.roles.utils.TestValues.readValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Set;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementClient;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@TestPropertySource(properties = {
  "application.kafka.filtering.tenant-filter.enabled=true",
  "application.retry.capability-event.retry-delay=10ms"
})
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-permission-table.sql"
})
class KafkaMessageFilteringIT extends BaseIntegrationTest {

  private static final String DISABLED_TENANT = "disabled";

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @MockitoBean private TenantEntitlementClient tenantEntitlementClient;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  /**
   * Testing the case when message for disabled tenant is ignored.
   * Verifies that:
   * <ul>
   *   <li>A message for a disabled tenant is filtered out and not processed.</li>
   *   <li>The next message for an enabled tenant is processed normally.</li>
   * </ul>
   */
  @Test
  void shouldFilterMessageForDisabledTenant_andProcessNextMessageForEnabledTenant() {
    // given: only TENANT_ID is enabled
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(Set.of(TENANT_ID));

    // when: sending a message for the disabled tenant followed by a message for the enabled tenant
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, eventForTenant(DISABLED_TENANT));
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, eventForTenant(TENANT_ID));

    // then: filter is consulted for both messages
    await().untilAsserted(() -> verify(tenantEntitlementClient, times(2)).lookupTenantsByModuleId(anyString()));

    // and: only the enabled tenant's message is processed — 5 capabilities are created
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
  }

  /**
   * Testing the case when no tenants are enabled initially (production-default FAIL strategy).
   * Verifies that:
   * <ul>
   *   <li>When no tenants are enabled, the filter throws {@code TenantsAreDisabledException},
   *       causing the message to be retried rather than silently discarded.</li>
   *   <li>As soon as a tenant becomes enabled, the pending message is processed on the next retry.</li>
   * </ul>
   */
  @Test
  void shouldRetryMessagesWhenNoTenantsEnabled_thenProcessWhenTenantBecomesEnabled() throws Exception {
    // given: no tenants are enabled — filter throws TenantsAreDisabledException → message is retried
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(null);

    var event = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);

    // when: a message is sent while no tenant is enabled
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, event);

    // then: the filter is called more than once — proves the message is being retried, not discarded
    await().untilAsserted(() -> verify(tenantEntitlementClient, atLeast(3)).lookupTenantsByModuleId(anyString()));

    // and: no capabilities created while retrying
    doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(0)));

    // when: tenant becomes enabled
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(Set.of(TENANT_ID));

    // then: the pending message is processed on the next retry — capabilities are created
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
  }

  /**
   * Testing the case when messages for enabled and disabled tenants are interleaved in the topic.
   * Verifies that when the topic contains messages for multiple tenants:
   * <ul>
   *   <li>Messages for tenants not in the enabled set are filtered out.</li>
   *   <li>Messages for the single enabled tenant are processed.</li>
   *   <li>The total count of processed capabilities reflects only the enabled tenant's events.</li>
   * </ul>
   */
  @Test
  void shouldFilterMessagesForNonEnabledTenants_whileProcessingForEnabledTenant() {
    // given: TENANT_ID is enabled; DISABLED_TENANT is not
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(Set.of(TENANT_ID));

    // when: three messages are sent — two for a disabled tenant, one for the enabled tenant
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, eventForTenant(DISABLED_TENANT));
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, eventForTenant(DISABLED_TENANT));
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, eventForTenant(TENANT_ID));

    // then: filter is consulted for all three messages
    await().untilAsserted(() -> verify(tenantEntitlementClient, times(3)).lookupTenantsByModuleId(anyString()));

    // and: only the enabled tenant's message produces capabilities
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
  }

  private ResourceEvent<CapabilityEvent> eventForTenant(String tenant) {
    var base = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);
    return ResourceEvent.<CapabilityEvent>builder()
      .type(base.getType())
      .tenant(tenant)
      .resourceName(base.getResourceName())
      .newValue((CapabilityEvent) base.getNewValue())
      .build();
  }
}
