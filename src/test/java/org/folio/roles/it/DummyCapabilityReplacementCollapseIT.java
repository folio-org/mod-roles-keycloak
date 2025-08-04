package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
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

@Log4j2
@IntegrationTest
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-permission-table.sql"
})
class DummyCapabilityReplacementCollapseIT extends BaseIntegrationTest {

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
  @DisplayName("Remove old dummy capabilities if it were replaced")
  void replacement_positive_updatedSetHasNewCapabilityAndOldShouldBeDeleted() throws Exception {
    sendCapabilityEvent("json/kafka-events/mod-items-1.0.0-create-event.json");

    var createdSetId = waitForCapabilitySetCreatedAndReturnId("set.items.view");
    waitForCapabilitySetHasPermissions(createdSetId, List.of(
      "set.items.view",
      "ui-orders.old.manage" //dummy, should be deleted after replacement
    ));

    sendCapabilityEvent("json/kafka-events/mod-items-2.0.0-update-event.json");

    waitForCapabilitySetHasPermissions(createdSetId, List.of(
      "set.items.view",
      "ui-orders.new.manage" //dummy
    ));

    sendCapabilityEvent("json/kafka-events/ui-app-create-event.json");

    waitForCapabilitySetHasPermissions(createdSetId, List.of(
      "set.items.view",
      "ui-orders.new.manage" //not dummy
    ));

    waitSystemHasNoCapability("ui-orders.old.manage");
  }

  @Test
  @DisplayName("Replace dummy capability when its real one is created")
  void replacement_positive_updatedSetHasOldCapabilityAndOldShouldBeDeleted() throws Exception {
    sendCapabilityEvent("json/kafka-events/mod-items-1.0.0-create-event.json");

    var createdSetId = waitForCapabilitySetCreatedAndReturnId("set.items.view");
    waitForCapabilitySetHasPermissions(createdSetId, List.of(
      "set.items.view",
      "ui-orders.old.manage" //dummy, will be replaced by real one (ui-orders.new.manage)
    ));

    sendCapabilityEvent("json/kafka-events/ui-app-create-event.json");

    waitForCapabilitySetHasPermissions(createdSetId, List.of(
      "set.items.view",
      "ui-orders.new.manage" //not dummy
    ));

    waitSystemHasNoCapability("ui-orders.old.manage");
  }

  private void sendCapabilityEvent(String filePath) {
    var capabilityEvent = readValue(filePath, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static String waitForCapabilitySetCreatedAndReturnId(String permission) {
    return await()
      .until(() -> {
        var mvcResult = doGet("/capability-sets?query=permission==" + permission).andReturn();
        var resp = parseResponse(mvcResult, CapabilitySets.class);
        if (resp.getTotalRecords() == 0) {
          return null;
        }
        return resp.getCapabilitySets().getFirst().getId().toString();
      }, is(notNullValue()));
  }

  private static void waitForCapabilitySetHasPermissions(String setId, List<String> permissions) {
    await()
      .untilAsserted(() -> {
        var expandedCapabilitiesForSet = getExpandedCapabilitiesForSet(setId);
        var setPermissions = mapItems(expandedCapabilitiesForSet, Capability::getPermission);
        assertThat(setPermissions).containsExactlyInAnyOrderElementsOf(permissions);
      });
  }

  private static void waitSystemHasNoCapability(String permissionName) {
    await()
      .untilAsserted(() -> {
        var allCapabilities = getAllCapabilities();
        var capability = allCapabilities.stream()
          .filter(c -> c.getPermission().equals(permissionName))
          .findFirst();
        assertThat(capability).isEmpty();
      });
  }

  private static List<Capability> getExpandedCapabilitiesForSet(String setId) throws Exception {
    var mvcResult = doGet("/capability-sets/{id}/capabilities?includeDummy=true", setId).andReturn();
    return parseResponse(mvcResult, Capabilities.class).getCapabilities();
  }

  private static List<Capability> getAllCapabilities() throws Exception {
    var mvcResult = doGet("/capabilities").andReturn();
    return parseResponse(mvcResult, Capabilities.class).getCapabilities();
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(TEN_SECONDS).pollInterval(FIVE_HUNDRED_MILLISECONDS);
  }
}
