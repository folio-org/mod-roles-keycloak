package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.TestUtils.parseResponse;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.util.UUID;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-permission-table.sql"
})
class CapabilityReplacesIntegrityIT extends BaseIntegrationTest {

  private static final UUID CORRUPTED_CAPABILITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID LEGITIMATE_OLD_CAPABILITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final String PERMISSION_TO_REPLACE = "old-feature.item.view";

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @CsvSource({
    "json/kafka-events/corrupted-capabilities/replacement-event-create.json",
    "json/kafka-events/corrupted-capabilities/replacement-event-update.json"
  })
  @ParameterizedTest(name = "[{index}] name={0}")
  @DisplayName("Verify that 'replaces' does not delete a corrupted capability")
  @Sql(scripts = "classpath:/sql/corrupted-capabilities/populate-data-replacement.sql")
  void replacement_positive_shouldNotDeleteCorruptedCapability(String eventPath) throws Exception {
    // Step 1: verify initial state
    var corruptedCapability = getCapabilityById(CORRUPTED_CAPABILITY_ID);
    assertThat(corruptedCapability.getPermission()).isEqualTo(PERMISSION_TO_REPLACE);

    var legitimateOldCapability = getCapabilityById(LEGITIMATE_OLD_CAPABILITY_ID);
    assertThat(legitimateOldCapability.getPermission()).isEqualTo(PERMISSION_TO_REPLACE);

    // Step 2: send event that replaces the "old-feature.item.view" permission
    sendCapabilityEvent(eventPath);

    // Step 3: assert that the corrupted capability is not deleted
    await().untilAsserted(() -> {
      var capabilities = getCapabilitiesByPermission(PERMISSION_TO_REPLACE);
      assertThat(capabilities.getTotalRecords()).isEqualTo(2);
      assertThat(mapItems(capabilities.getCapabilities(), Capability::getId)).contains(CORRUPTED_CAPABILITY_ID);
    });
  }

  private void sendCapabilityEvent(String filePath) {
    var capabilityEvent = readValue(filePath, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static Capability getCapabilityById(UUID id) throws Exception {
    var mvcResult = doGet("/capabilities/{id}?includeDummy=true", id).andReturn();
    return parseResponse(mvcResult, Capability.class);
  }

  private static Capabilities getCapabilitiesByPermission(String permission) throws Exception {
    var mvcResult = doGet("/capabilities?includeDummy=true&query=permission==" + permission).andReturn();
    return parseResponse(mvcResult, Capabilities.class);
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(ONE_MINUTE).pollInterval(FIVE_HUNDRED_MILLISECONDS);
  }
}
