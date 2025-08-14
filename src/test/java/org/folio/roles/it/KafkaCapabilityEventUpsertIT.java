package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
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
import org.folio.roles.domain.dto.CapabilitySet;
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
class KafkaCapabilityEventUpsertIT extends BaseIntegrationTest {

  private static final UUID CAPABILITY_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final String CORRUPTED_PERMISSION_NAME = "wrong.capability.permission";
  private static final String CORRECT_CAPABILITY_PERMISSION_NAME = "test.sub-capability.view";
  private static final UUID DUMMY_CAPABILITY_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final String CORRECT_DUMMY_CAPABILITY_PERMISSION = "test.dummy.capability.view";
  private static final UUID CAPABILITY_SET_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final String CORRECT_CAPABILITY_SET_PERMISSION = "test.capability-set.all";

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
    "json/kafka-events/corrupted-capabilities/correct-capability-event-create.json",
    "json/kafka-events/corrupted-capabilities/correct-capability-event-update.json"
  })
  @ParameterizedTest(name = "[{index}] name={0}")
  @DisplayName("Verify that a corrupted capabilities data is fixed by a new Kafka event")
  @Sql(scripts = "classpath:/sql/corrupted-capabilities/populate-corrupted-capabilities-and-sets.sql")
  void handleCapabilityEvent_positive_fixCorruptedCapabilitiesData(String eventPath) throws Exception {
    // Step 1: Verify initial corrupted state via API
    var initialCapability = getCapabilityById(CAPABILITY_ID);
    assertThat(initialCapability.getPermission()).isEqualTo(CORRUPTED_PERMISSION_NAME);
    var capabilities = getCapabilitiesByPermission(CORRUPTED_PERMISSION_NAME);
    assertThat(capabilities.getTotalRecords()).isEqualTo(1);

    // Step 2: Send repairing Kafka event
    sendCapabilityEvent(eventPath);

    // Step 3: Wait and verify that the capability has been repaired
    await().untilAsserted(() -> {
      // Assert that the capability has been updated with the correct permission
      var repearedCapability = getCapabilityById(CAPABILITY_ID);
      assertThat(repearedCapability.getPermission()).isEqualTo(CORRECT_CAPABILITY_PERMISSION_NAME);
      assertThat(repearedCapability.getDummyCapability()).isFalse();

      // Assert that the capability set has been updated with the correct permission
      var capabilitySet = getCapabilitySetById(CAPABILITY_SET_ID);
      assertThat(capabilitySet.getPermission()).isEqualTo(CORRECT_CAPABILITY_SET_PERMISSION);

      // Assert that the dummy capability has been update with the correct permission
      var dummyCapability = getCapabilityById(DUMMY_CAPABILITY_ID);
      assertThat(dummyCapability.getPermission()).isEqualTo(CORRECT_DUMMY_CAPABILITY_PERMISSION);
    });
  }

  private void sendCapabilityEvent(String filePath) {
    var capabilityEvent = readValue(filePath, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static Capability getCapabilityById(UUID id) throws Exception {
    var mvcResult = doGet("/capabilities/{id}", id).andReturn();
    return parseResponse(mvcResult, Capability.class);
  }

  private static Capabilities getCapabilitiesByPermission(String permission) throws Exception {
    var mvcResult = doGet("/capabilities?includeDummy=true&query=permission==" + permission).andReturn();
    return parseResponse(mvcResult, Capabilities.class);
  }

  private static CapabilitySet getCapabilitySetById(UUID id) throws Exception {
    var mvcResult = doGet("/capability-sets/{id}", id).andReturn();
    return parseResponse(mvcResult, CapabilitySet.class);
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(ONE_MINUTE).pollInterval(FIVE_HUNDRED_MILLISECONDS);
  }
}
