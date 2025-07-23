package org.folio.roles.it;

import static com.github.jknack.handlebars.internal.lang3.BooleanUtils.isNotTrue;
import static com.github.jknack.handlebars.internal.lang3.BooleanUtils.isTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;

@IntegrationTest
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-permission-table.sql",
  "classpath:/sql/truncate-capability-tables.sql",
})
class DummyCapabilityIT extends BaseIntegrationTest {

  private static final String CAPABILITY_SET_PERMISSION = "test.real.set-from-event.all";
  private static final String REAL_CAPABILITY_PERMISSION = "test.real.from-event.view";
  private static final String REPLACED_DUMMY_CAPABILITY_PERMISSION = "test.replace.from-event.edit";
  private static final String DUMMY_CAPABILITY_PERMISSION = "foo.dummy.from-event.edit";

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @DisplayName("Handle capability event and create dummy capability")
  void handleCapabilityEvent_positive_dummyCapabilityCreation() throws Exception {
    sendCapabilityEvent("json/kafka-events/dummy-capability-set-event.json");

    var capabilitySetId = waitForCapabilitySetCreated("test.real.set-from-event.all");
    var capabilities = getCapabilitiesForSet(capabilitySetId);

    assertThat(capabilities).hasSize(3);
    assertRealCapability(capabilities);
    assertDummyCapability(capabilities);
  }

  @Test
  @DisplayName("Create dummy capability then handle event with real capability")
  void handleCapabilityEvent_positive_realCapabilityCreation() throws Exception {
    sendCapabilityEvent("json/kafka-events/dummy-capability-set-event.json");
    var capabilitySetId = waitForCapabilitySetCreated(CAPABILITY_SET_PERMISSION);
    var capabilities = getCapabilitiesForSet(capabilitySetId);

    assertThat(capabilities).hasSize(3);
    assertRealCapability(capabilities);
    assertDummyCapability(capabilities);

    sendCapabilityEvent("json/kafka-events/dummy-capability-replaced-real-capability-event.json");

    //wait for event to be processed by getting capability by permission name and checking its dummy capability flag
    waitUntilDummyCapabilityBecomesReal();

    capabilities = getCapabilitiesForSet(capabilitySetId);
    assertAllCapabilitiesReal(capabilities);
  }

  @Test
  @DisplayName("Create dummy capability and then handle replace permission event")
  void handleCapabilityEvent_positive_dummyCapabilityReplacePermissionEvent() throws Exception {
    sendCapabilityEvent("json/kafka-events/dummy-capability-set-event.json");

    var capabilitySetId = waitForCapabilitySetCreated("test.real.set-from-event.all");
    var capabilities = getCapabilitiesForSet(capabilitySetId);

    assertThat(capabilities).hasSize(3);
    assertRealCapability(capabilities);
    assertDummyCapability(capabilities);

    sendCapabilityEvent("json/kafka-events/dummy-capability-replaces-permission-event.json");

    assertRealCapabilityPermissionsAfterPermissionReplacement(capabilitySetId);
  }

  private void sendCapabilityEvent(String filePath) {
    var capabilityEvent = readValue(filePath, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static void waitUntilDummyCapabilityBecomesReal() {
    await().atMost(1, TimeUnit.MINUTES)
      .pollInterval(1, TimeUnit.SECONDS)
      .until(() -> {
        var mvcResult = doGet("/capabilities?query=permission==" + DUMMY_CAPABILITY_PERMISSION).andReturn();
        return parseResponse(mvcResult, new TypeReference<List<Capability>>() {}).stream()
          .filter(c -> isNotTrue(c.getDummyCapability()))
          .findFirst()
          .orElse(null);
      }, is(notNullValue()));
  }

  private static String waitForCapabilitySetCreated(String permission) {
    return await().atMost(1, TimeUnit.MINUTES)
      .pollInterval(1, TimeUnit.SECONDS)
      .until(() -> {
        var mvcResult = doGet("/capability-sets?query=permission==" + permission).andReturn();
        var resp = parseResponse(mvcResult, CapabilitySets.class);
        return resp.getCapabilitySets().getFirst().getId().toString();
      }, is(notNullValue()));
  }

  private static List<Capability> getCapabilitiesForSet(String setId) throws Exception {
    var mvcResult = doGet("/capability-sets/{id}/capabilities?includeDummy=true", setId).andReturn();
    return parseResponse(mvcResult, Capabilities.class).getCapabilities();
  }

  private static void assertRealCapability(List<Capability> capabilities) {
    var permissionsForRealCapabilities = capabilities.stream()
      .filter(c -> isNotTrue(c.getDummyCapability()))
      .map(Capability::getPermission)
      .toList();

    assertThat(permissionsForRealCapabilities).hasSize(2);
    assertThat(permissionsForRealCapabilities).contains(REAL_CAPABILITY_PERMISSION, CAPABILITY_SET_PERMISSION);
  }

  private void assertRealCapabilityPermissionsAfterPermissionReplacement(String setId) {
    await().atMost(1, TimeUnit.MINUTES)
      .pollInterval(1, TimeUnit.SECONDS)
      .until(() -> {
        var capabilities = getCapabilitiesForSet(setId);
        return capabilities.stream()
          .filter(c -> isNotTrue(c.getDummyCapability()))
          .map(Capability::getPermission)
          .toList();
      }, containsInAnyOrder(REAL_CAPABILITY_PERMISSION, CAPABILITY_SET_PERMISSION,
        REPLACED_DUMMY_CAPABILITY_PERMISSION));
  }

  private static void assertDummyCapability(List<Capability> capabilities) {
    var permissionsForDummyCapabilities = capabilities.stream()
      .filter(c -> isTrue(c.getDummyCapability()))
      .map(Capability::getPermission)
      .toList();

    assertThat(permissionsForDummyCapabilities).hasSize(1);
    assertThat(permissionsForDummyCapabilities).contains(DUMMY_CAPABILITY_PERMISSION);
  }

  private static void assertAllCapabilitiesReal(List<Capability> capabilities) {
    var permissionsForRealCapabilities = capabilities.stream()
      .filter(c -> isNotTrue(c.getDummyCapability()))
      .map(Capability::getPermission)
      .toList();

    assertThat(permissionsForRealCapabilities).hasSize(3);
    assertThat(permissionsForRealCapabilities).contains(REAL_CAPABILITY_PERMISSION, CAPABILITY_SET_PERMISSION,
      DUMMY_CAPABILITY_PERMISSION);
  }
}
