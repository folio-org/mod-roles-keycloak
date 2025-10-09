package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.test.TestUtils.parseResponse;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.UserRolesRequest;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

@Log4j2
@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-loadable-tables.sql",
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql",
  "classpath:/sql/truncate-roles-user-related-tables.sql"})
class LoadableRolesWithDummyCapabilitiesIT extends BaseIntegrationTest {

  private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private Keycloak keycloak;

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
    keycloak.tokenManager().grantToken();
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  @DisplayName("User default (loadable) role with dummy capability set is loaded correctly")
  void userLoadableRole_positive_loadableRoleWithNestedDummyCapabilitySet() throws Exception {
    final var permissionSetLevel1 = "ui-level-1.all";
    final var permissionRealLevel1 = "real.permission.get";
    final var permissionDummySetLevel2 = "ui-level-2.all";
    final var permissionNestedSetLevel2 = "nested.permission.get";
    final var permissionNestedDummyLevel3 = "nested.dummy.permission.post";

    // Step 1: Create a loadable role that requires a capability set which, in turn, contains a dummy set
    var loadableRole = new LoadableRole().name("Nested Dummy Set Role")
      .description("Role to test nested dummy capability sets")
      .permissions(List.of(new LoadablePermission().permissionName("ui-level-1.all")));

    var createdLoadableRole = parseResponse(doPut("/loadable-roles", loadableRole).andReturn(), LoadableRole.class);

    // Step 2: Assign role to the user
    var userRoleRequest =
      new UserRolesRequest().userId(UUID.fromString(USER_ID)).roleIds(List.of(createdLoadableRole.getId()));
    doPost("/roles/users", userRoleRequest);

    // Step 3: Send the first event, creating a real set with a nested dummy set
    sendCapabilityEvent("json/kafka-events/loadable-role-nested-dummy-set-event.json");

    // Step 4: Verify that the user initially has no dummy capabilities from the first-level set
    await().untilAsserted(() -> {
      var userPermissions = getUserPermissions(USER_ID);
      assertThat(userPermissions).containsExactlyInAnyOrder(permissionSetLevel1, permissionRealLevel1);
    });

    // Step 5: Send the second event, which resolves the nested dummy capability set
    sendCapabilityEvent("json/kafka-events/loadable-role-real-nested-set-event.json");

    // Step 6: Verify that the user now has permissions from both the even1 and event2 excluding the dummy capability
    await().untilAsserted(() -> {
      var userPermissions = getUserPermissions(USER_ID);
      assertThat(userPermissions).containsExactlyInAnyOrder(permissionSetLevel1, permissionRealLevel1,
        permissionNestedSetLevel2, permissionDummySetLevel2);
    });

    // Step 7: Send the third event, which resolves the final nested dummy capability
    sendCapabilityEvent("json/kafka-events/loadable-role-real-nested-dummy-capability-event.json");

    // Step 8: Verify that the user now has all permissions, including the one from the final event
    await().untilAsserted(() -> {
      var userPermissions = getUserPermissions(USER_ID);
      assertThat(userPermissions).containsExactlyInAnyOrder(permissionSetLevel1, permissionRealLevel1,
        permissionNestedSetLevel2, permissionDummySetLevel2, permissionNestedDummyLevel3);
    });
  }

  private void sendCapabilityEvent(String file) {
    var capabilityEvent = readValue(file, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(ONE_MINUTE).pollInterval(FIVE_HUNDRED_MILLISECONDS);
  }

  private List<String> getUserPermissions(String userId) throws Exception {
    var mvcResult = doGet(get("/permissions/users/{userId}", userId).header(TENANT, TENANT_ID)).andReturn();

    var permissions = parseResponse(mvcResult, UserPermissions.class);
    return permissions.getPermissions();
  }
}
