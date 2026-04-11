package org.folio.roles.it;

import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestUtils.await;
import static org.folio.roles.utils.TestValues.readValue;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.util.Set;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementClient;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.roles.base.BaseIntegrationTest;
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
  "application.kafka.filtering.tenant-filter.tenant-disabled-strategy=FAIL",
  "application.retry.capability-event.retry-delay=10ms"
})
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-permission-table.sql"
})
class KafkaMessageFilteringFailStrategyIT extends BaseIntegrationTest {

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
   * Verifies the FAIL strategy for a specific disabled tenant ({@code tenant-disabled-strategy=FAIL}).
   * The following scenario is tested:
   * <ul>
   *   <li>The entitlement service returns a non-empty enabled-tenant set that does not include
   *       the message's tenant, so the filter throws {@code TenantIsDisabledException}.</li>
   *   <li>The exception triggers retries (the error handler classifies it as retryable),
   *       so the client is called more than once — the message is not lost.</li>
   *   <li>When the tenant is eventually added to the enabled set, the message is processed on the
   *       next retry and capabilities are created.</li>
   * </ul>
   */
  @Test
  void shouldRetryMessage_untilTenantBecomesEnabled_andThenProcessIt() throws Exception {
    // given: enabled set does not contain TENANT_ID — filter throws TenantIsDisabledException
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(Set.of("other-tenant"));

    var event = readValue("json/kafka-events/be-capability-event.json", ResourceEvent.class);

    // when: a message for TENANT_ID arrives while it is not yet in the enabled set
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, event);

    // then: the filter is called more than once — proves the message is being retried, not discarded
    await().untilAsserted(() -> verify(tenantEntitlementClient, atLeast(3)).lookupTenantsByModuleId(anyString()));

    // and: no capabilities created while the message is retrying
    doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(0)));

    // when: TENANT_ID is added to the enabled set
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(Set.of(TENANT_ID));

    // then: the pending message is processed on the next retry — capabilities are created
    await().untilAsserted(() -> doGet("/capabilities").andExpect(jsonPath("$.totalRecords", is(5))));
  }
}
