package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

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
class NestedCapabilitySetIT extends BaseIntegrationTest {

  private static final String FIRST_MODULE_LEVEL1_SET = "first.module.all";
  private static final String FIRST_MODULE_LEVEL2_SET = "first.module.execute";
  private static final String FIRST_MODULE_REAL_PERMISSION = "first.module.real.get";
  private static final String SECOND_MODULE_LEVEL1_SET = "second.module.all";
  private static final String SECOND_MODULE_REAL_PERMISSION_GET = "second.module.real.get";
  private static final String SECOND_MODULE_REAL_PERMISSION_DELETE = "second.module.real.delete";

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
  @DisplayName("Handle event with dummy capability sets and a dummy capability")
  void handleKafkaCapabilityEvent_positive_dummyCapabilityIsCreatedWithCorrectPermission() throws Exception {
    sendCapabilityEvent("json/kafka-events/nested-capability-set-event.json");

    var capabilitySetId = waitForCapabilitySetCreated(FIRST_MODULE_LEVEL1_SET);
    var capabilities = getExpandedCapabilitiesForSet(capabilitySetId);

    assertThat(capabilities).hasSize(4);
    var capabilityPermissions = mapItems(capabilities, Capability::getPermission);
    assertThat(capabilityPermissions).containsExactlyInAnyOrder(
      FIRST_MODULE_LEVEL1_SET,
      FIRST_MODULE_LEVEL2_SET,
      FIRST_MODULE_REAL_PERMISSION,
      SECOND_MODULE_LEVEL1_SET
    );
    var dummyCapability =
      toStream(capabilities).filter(c -> c.getPermission().equals(SECOND_MODULE_LEVEL1_SET)).findFirst().get();
    assertThat(dummyCapability.getDummyCapability()).isTrue();
  }

  @Test
  @DisplayName("Handles event with deeper nested dummy capability sets")
  void handleKafkaCapabilityEvent_positive_deeperDummyCapabilityIsCreated() throws Exception {
    sendCapabilityEvent("json/kafka-events/nested-capability-set-event.json");
    var mainCapabilitySetId = waitForCapabilitySetCreated(FIRST_MODULE_LEVEL1_SET);

    sendCapabilityEvent("json/kafka-events/nested-capability-set-deeper-event.json");
    waitForCapabilitySetCreated(SECOND_MODULE_LEVEL1_SET);

    var capabilities = getExpandedCapabilitiesForSet(mainCapabilitySetId);

    assertThat(capabilities).hasSize(6);
    var capabilityPermissions = mapItems(capabilities, Capability::getPermission);
    assertThat(capabilityPermissions).containsExactlyInAnyOrder(
      FIRST_MODULE_LEVEL1_SET,
      FIRST_MODULE_LEVEL2_SET,
      FIRST_MODULE_REAL_PERMISSION,
      SECOND_MODULE_LEVEL1_SET,
      SECOND_MODULE_REAL_PERMISSION_GET,
      SECOND_MODULE_REAL_PERMISSION_DELETE
    );
  }

  @Test
  @DisplayName("Handle events with cyclic nested capability sets (dummy capabilities)")
  void handleKafkaCapabilityEvent_positive_noInfiniteLoopAndCorrectCapabilitiesResolved() throws Exception {
    final var cyclicSetA = "a.cyclic.set.all";
    final var cyclicSetB = "b.cyclic.set.all";
    final var cyclicDummyA = "a.cyclic.dummy.view";
    final var cyclicDummyB = "b.cyclic.dummy.view";
    final var cyclicRealA = "a.cyclic.real.get";
    final var cyclicRealB = "b.cyclic.real.post";

    sendCapabilityEvent("json/kafka-events/nested-capability-set-cyclic-event.json");

    var capabilitySetIdA = waitForCapabilitySetCreated(cyclicSetA);
    var capabilitySetIdB = waitForCapabilitySetCreated(cyclicSetB);

    var capabilitiesA = getExpandedCapabilitiesForSet(capabilitySetIdA);
    var capabilitiesB = getExpandedCapabilitiesForSet(capabilitySetIdB);

    assertThat(capabilitiesA).hasSize(6);
    assertThat(capabilitiesB).hasSize(6);

    var expectedPermissions = List.of(
      cyclicSetA, cyclicSetB, cyclicDummyA, cyclicDummyB, cyclicRealA, cyclicRealB);

    var capabilityPermissionsA = mapItems(capabilitiesA, Capability::getPermission);
    assertThat(capabilityPermissionsA).containsExactlyInAnyOrderElementsOf(expectedPermissions);

    var capabilityPermissionsB = mapItems(capabilitiesB, Capability::getPermission);
    assertThat(capabilityPermissionsB).containsExactlyInAnyOrderElementsOf(expectedPermissions);
  }

  private void sendCapabilityEvent(String filePath) {
    var capabilityEvent = readValue(filePath, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static String waitForCapabilitySetCreated(String permission) {
    return await().atMost(1, TimeUnit.MINUTES)
      .pollInterval(1, TimeUnit.SECONDS)
      .until(() -> {
        var mvcResult = doGet("/capability-sets?query=permission==" + permission).andReturn();
        var resp = parseResponse(mvcResult, CapabilitySets.class);
        if (resp.getTotalRecords() == 0) {
          return null;
        }
        return resp.getCapabilitySets().getFirst().getId().toString();
      }, is(notNullValue()));
  }

  private static List<Capability> getExpandedCapabilitiesForSet(String setId) throws Exception {
    var mvcResult = doGet("/capability-sets/{id}/capabilities?expand=true&includeDummy=true", setId).andReturn();
    return parseResponse(mvcResult, Capabilities.class).getCapabilities();
  }
}
