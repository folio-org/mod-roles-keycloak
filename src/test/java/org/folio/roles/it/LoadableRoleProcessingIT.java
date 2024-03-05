package org.folio.roles.it;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.TWO_HUNDRED_MILLISECONDS;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.parseResponse;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.assertj.core.api.ThrowingConsumer;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.integration.kafka.CapabilityConverterUtils;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.utils.CapabilityUtils;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.jdbc.JdbcTestUtils;

@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-loadable-tables.sql",
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql"
})
public class LoadableRoleProcessingIT extends BaseIntegrationTest {

  private static final String CIRC_MANAGER_ROLE_NAME = "Circulation Manager";
  private static final String CIRC_STUDENT_ROLE_NAME = "Circulation Student";
  private static final String ROLE_LOADABLE_PERMISSION_TABLE = "test_mod_roles_keycloak.role_loadable_permission";

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void beforeAll() {
    enableTenant(TENANT_ID);
  }

  @AfterAll
  static void afterAll() {
    removeTenant(TENANT_ID);
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  @Sql(scripts = "classpath:/sql/populate-role-loadable-with-ui-capsets.sql")
  void assignCapabilitySets_positive_fromOneModule() throws Exception {
    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");
    sendCapabilityEvent("json/kafka-events/ui-notes-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilitySetCountForPermissionLike("ui-notes");
      assertThat(count).isEqualTo(0);
    });

    var capSetByName = getExistingCapabilitySets();

    verifyAssignedCapabilitySets(CIRC_MANAGER_ROLE_NAME, role -> selectPermissionsLike(role, "ui-notes"), capSetByName);
    verifyAssignedCapabilitySets(CIRC_STUDENT_ROLE_NAME, role -> selectPermissionsLike(role, "ui-notes"), capSetByName);
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  @Sql(scripts = "classpath:/sql/populate-role-loadable-with-ui-capsets.sql")
  void assignCapabilitySets_positive_fromTwoModule_sequentially() throws Exception {
    // notes' events
    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");
    sendCapabilityEvent("json/kafka-events/ui-notes-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilitySetCountForPermissionLike("ui-notes");
      assertThat(count).isEqualTo(0);
    });

    // tags' events
    sendCapabilityEvent("json/kafka-events/be-tags-capability-event.json");
    sendCapabilityEvent("json/kafka-events/ui-tags-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilitySetCountForPermissionLike("ui-tags");
      assertThat(count).isEqualTo(0);
    });

    var capSetByName = getExistingCapabilitySets();

    verifyAssignedCapabilitySets(CIRC_MANAGER_ROLE_NAME,
      role -> selectPermissionsLike(role, "ui-notes", "ui-tags"), capSetByName);
    verifyAssignedCapabilitySets(CIRC_STUDENT_ROLE_NAME,
      role -> selectPermissionsLike(role, "ui-notes", "ui-tags"), capSetByName);
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  @Sql(scripts = "classpath:/sql/populate-role-loadable-with-be-capabilities.sql")
  void assignCapabilities_positive_fromOneModule() throws Exception {
    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");
    sendCapabilityEvent("json/kafka-events/ui-notes-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilityCountForPermissionLike("notes.item");
      assertThat(count).isEqualTo(0);
    });

    var capabilitiesByName = getExistingCapabilities();

    verifyAssignedCapabilities(CIRC_MANAGER_ROLE_NAME,
      role -> selectPermissionsLike(role, "notes.item"), capabilitiesByName);
    verifyAssignedCapabilities(CIRC_STUDENT_ROLE_NAME,
      role -> selectPermissionsLike(role, "notes.item"), capabilitiesByName);
  }

  private int unassignedCapabilitySetCountForPermissionLike(String permission) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, ROLE_LOADABLE_PERMISSION_TABLE,
      format("folio_permission LIKE '%%%s%%' and capability_set_id IS NULL", permission));
  }

  private int unassignedCapabilityCountForPermissionLike(String permission) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, ROLE_LOADABLE_PERMISSION_TABLE,
      format("folio_permission LIKE '%%%s%%' and capability_id IS NULL", permission));
  }

  private void sendCapabilityEvent(String file) {
    var capabilityEvent = readValue(file, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, capabilityEvent);
  }

  private static void verifyAssignedCapabilitySets(String roleName,
    Function<LoadableRole, List<LoadablePermission>> permissionSelector,
    Map<String, CapabilitySet> capSetByName) throws Exception {
    var cmRole = getLoadableRoleByName(roleName);
    var selectedPermissions = permissionSelector.apply(cmRole);

    assertThat(selectedPermissions)
      .isNotEmpty()
      .allSatisfy(capabilitySetAssignedToOneFrom(capSetByName));
  }

  private static void verifyAssignedCapabilities(String roleName,
    Function<LoadableRole, List<LoadablePermission>> permissionSelector,
    Map<String, Capability> capabilityByName) throws Exception {
    var cmRole = getLoadableRoleByName(roleName);
    var selectedPermissions = permissionSelector.apply(cmRole);

    assertThat(selectedPermissions)
      .isNotEmpty()
      .allSatisfy(capabilityAssignedToOneFrom(capabilityByName));
  }

  private static ThrowingConsumer<LoadablePermission> capabilitySetAssignedToOneFrom(
    Map<String, CapabilitySet> capSetByName) {
    return p -> {
      assertThat(p.getCapabilitySetId()).isNotNull();

      var name = p.getPermissionName();
      var rawCap = CapabilityConverterUtils.getRawCapability(name);
      var capabilitySetName = CapabilityUtils.getCapabilityName(rawCap);

      var capabilitySet = capSetByName.get(capabilitySetName);
      assertThat(capabilitySet).isNotNull();
      assertThat(p.getCapabilitySetId()).isEqualTo(capabilitySet.getId());
    };
  }

  private static ThrowingConsumer<LoadablePermission> capabilityAssignedToOneFrom(
    Map<String, Capability> capabilityByName) {
    return p -> {
      assertThat(p.getCapabilityId()).isNotNull();

      var name = p.getPermissionName();
      var rawCap = CapabilityConverterUtils.getRawCapability(name);
      var capabilityName = CapabilityUtils.getCapabilityName(rawCap);

      var capability = capabilityByName.get(capabilityName);
      assertThat(capability).isNotNull();
      assertThat(p.getCapabilityId()).isEqualTo(capability.getId());
    };
  }

  private static List<LoadablePermission> selectPermissionsLike(LoadableRole cmRole, String... permissionMask) {
    var permissions = cmRole.getPermissions();
    return permissions.stream()
      .filter(p -> stream(permissionMask).anyMatch(mask -> p.getPermissionName().contains(mask)))
      .toList();
  }

  private static LoadableRole getLoadableRoleByName(String name) throws Exception {
    var mvcResult = doGet(get("/loadable-roles")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .queryParam("query", "name == " + name)
      .queryParam("limit", "100"))
      .andReturn();
    var loadableRoles = parseResponse(mvcResult, LoadableRoles.class).getLoadableRoles();
    assertThat(loadableRoles).isNotEmpty();
    assertThat(loadableRoles.size()).isEqualTo(1);
    return loadableRoles.get(0);
  }

  private static Map<String, CapabilitySet> getExistingCapabilitySets() throws Exception {
    var mvcResult = doGet(get("/capability-sets")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .queryParam("limit", "100"))
      .andReturn();
    var capabilitySets = parseResponse(mvcResult, CapabilitySets.class).getCapabilitySets();
    assertThat(capabilitySets).isNotEmpty();

    return capabilitySets.stream().collect(toMap(CapabilitySet::getName, identity()));
  }

  private static Map<String, Capability> getExistingCapabilities() throws Exception {
    var mvcResult = doGet(get("/capabilities")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .queryParam("limit", "100"))
      .andReturn();
    var capabilities = parseResponse(mvcResult, Capabilities.class).getCapabilities();
    assertThat(capabilities).isNotEmpty();

    return capabilities.stream().collect(toMap(Capability::getName, identity()));
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(ONE_MINUTE).pollInterval(TWO_HUNDRED_MILLISECONDS);
  }
}
