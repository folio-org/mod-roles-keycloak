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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
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
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.jdbc.JdbcTestUtils;

@Log4j2
@IntegrationTest
@SqlMergeMode(MERGE)
@Sql(executionPhase = AFTER_TEST_METHOD, scripts = {
  "classpath:/sql/truncate-role-loadable-tables.sql",
  "classpath:/sql/truncate-role-tables.sql",
  "classpath:/sql/truncate-policy-tables.sql",
  "classpath:/sql/truncate-capability-tables.sql",
  "classpath:/sql/truncate-role-capability-tables.sql"
})
class LoadableRoleProcessingIT extends BaseIntegrationTest {

  private static final String CIRC_MANAGER_ROLE_NAME = "Circulation Manager";
  private static final String CIRC_STUDENT_ROLE_NAME = "Circulation Student";
  private static final String ROLE_LOADABLE_PERMISSION_TABLE = "test_mod_roles_keycloak.role_loadable_permission";

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
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
  @Sql(scripts = "classpath:/sql/populate-role-loadable-with-ui-capsets.sql")
  void assignCapabilitySets_positive_fromOneModule() throws Exception {
    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");
    sendCapabilityEvent("json/kafka-events/ui-notes-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilitySetCountForPermissionLike("ui-notes");
      assertThat(count).isZero();
    });

    var capSetByPermission = getExistingCapabilitySets();

    verifyAssignedCapabilitySets(
      CIRC_MANAGER_ROLE_NAME,
      role -> selectPermissionsLike(role, "ui-notes"),
      capSetByPermission);
    verifyAssignedCapabilitySets(
      CIRC_STUDENT_ROLE_NAME,
      role -> selectPermissionsLike(role, "ui-notes"),
      capSetByPermission);
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
      assertThat(count).isZero();
    });

    // tags' events
    sendCapabilityEvent("json/kafka-events/be-tags-capability-event.json");
    sendCapabilityEvent("json/kafka-events/ui-tags-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilitySetCountForPermissionLike("ui-tags");
      assertThat(count).isZero();
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
      assertThat(count).isZero();
    });

    var capabilitiesByPermission = getExistingCapabilities();

    verifyAssignedCapabilities(CIRC_MANAGER_ROLE_NAME,
      role -> selectPermissionsLike(role, "notes.item"),
      capabilitiesByPermission);
    verifyAssignedCapabilities(CIRC_STUDENT_ROLE_NAME,
      role -> selectPermissionsLike(role, "notes.item"),
      capabilitiesByPermission);
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void upsertLoadableRole_positive() throws Exception {
    var role = new LoadableRole()
      .name("Test Loadable Role")
      .description("Test description")
      .permissions(List.of(new LoadablePermission().permissionName("test.permission")));

    var mvcResult = doPut("/loadable-roles", role)
      .andExpect(status().isOk())
      .andReturn();

    var createdRole = parseResponse(mvcResult, LoadableRole.class);
    assertThat(createdRole.getName()).isEqualTo(role.getName());
    assertThat(createdRole.getDescription()).isEqualTo(role.getDescription());
    assertThat(createdRole.getPermissions()).hasSize(1);
    assertThat(createdRole.getPermissions().getFirst().getPermissionName()).isEqualTo("test.permission");
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void upsertLoadableRole_positive_updateExistingRole() throws Exception {
    var initialRole = new LoadableRole()
      .name("Update Test Role")
      .description("Initial description")
      .permissions(List.of(new LoadablePermission().permissionName("initial.permission")));

    var createResult = doPut("/loadable-roles", initialRole)
      .andExpect(status().isOk())
      .andReturn();

    var createdRole = parseResponse(createResult, LoadableRole.class);
    assertThat(createdRole.getId()).isNotNull();

    var updatedRole = new LoadableRole()
      .id(createdRole.getId())
      .name(createdRole.getName())
      .description("Updated description")
      .permissions(List.of(
        new LoadablePermission().permissionName("initial.permission"),
        new LoadablePermission().permissionName("additional.permission")
      ));

    var updateResult = doPut("/loadable-roles", updatedRole)
      .andExpect(status().isOk())
      .andReturn();

    var returnedRole = parseResponse(updateResult, LoadableRole.class);
    assertThat(returnedRole.getId()).isEqualTo(createdRole.getId());
    assertThat(returnedRole.getName()).isEqualTo("Update Test Role");
    assertThat(returnedRole.getDescription()).isEqualTo("Updated description");
    assertThat(returnedRole.getPermissions()).hasSize(2);

    var fetchedRole = getLoadableRoleByName("Update Test Role");
    assertThat(fetchedRole.getDescription()).isEqualTo("Updated description");
    assertThat(fetchedRole.getPermissions()).hasSize(2);
    assertThat(fetchedRole.getPermissions())
      .extracting(LoadablePermission::getPermissionName)
      .containsExactlyInAnyOrder("initial.permission", "additional.permission");
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void upsertLoadableRole_positive_populateRoleWithCapabilityFromEvent() throws Exception {
    var expectedPermission = "notes.collection.get";
    var role = new LoadableRole()
      .name("Note Reader Role")
      .description("Role for reading notes")
      .permissions(List.of(new LoadablePermission().permissionName(expectedPermission)));

    doPut("/loadable-roles", role)
      .andExpect(status().isOk());

    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");

    await().untilAsserted(() -> {
      var fetchedRole = getLoadableRoleByName("Note Reader Role");
      var permissions = fetchedRole.getPermissions();
      assertThat(permissions).isNotEmpty();
      var matchingPermission = permissions.stream()
        .filter(p -> p.getPermissionName().equals(expectedPermission))
        .findFirst()
        .orElseThrow();
      assertThat(matchingPermission.getCapabilityId()).isNotNull();
    });
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void upsertLoadableRole_positive_populateRoleWithExistingCapabilitySet() throws Exception {
    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");

    var expectedPermissionSet = "notes.allops";
    await().untilAsserted(() -> {
      var existingCapabilitySets = getExistingCapabilitySets();
      assertThat(existingCapabilitySets.get(expectedPermissionSet)).isNotNull();
    });

    var role = new LoadableRole()
      .name("Note Reader Role")
      .description("Role for reading notes")
      .permissions(List.of(new LoadablePermission().permissionName(expectedPermissionSet)));

    doPut("/loadable-roles", role)
      .andExpect(status().isOk());

    await().untilAsserted(() -> {
      var fetchedRole = getLoadableRoleByName("Note Reader Role");
      var permissions = fetchedRole.getPermissions();
      assertThat(permissions).isNotEmpty();
      var matchingPermission = permissions.stream()
        .filter(p -> p.getPermissionName().equals(expectedPermissionSet))
        .findFirst()
        .orElseThrow();
      assertThat(matchingPermission.getCapabilitySetId()).isNotNull();
    });
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
    Map<String, CapabilitySet> capSetByPermission) throws Exception {
    var cmRole = getLoadableRoleByName(roleName);
    var selectedPermissions = permissionSelector.apply(cmRole);

    assertThat(selectedPermissions)
      .isNotEmpty()
      .allSatisfy(capabilitySetAssignedToOneFrom(capSetByPermission));
  }

  private static void verifyAssignedCapabilities(String roleName,
    Function<LoadableRole, List<LoadablePermission>> permissionSelector,
    Map<String, Capability> capabilityByPermission) throws Exception {
    var cmRole = getLoadableRoleByName(roleName);
    var selectedPermissions = permissionSelector.apply(cmRole);

    assertThat(selectedPermissions)
      .isNotEmpty()
      .allSatisfy(capabilityAssignedToOneFrom(capabilityByPermission));
  }

  private static ThrowingConsumer<LoadablePermission> capabilitySetAssignedToOneFrom(
    Map<String, CapabilitySet> capSetByPermission) {
    return p -> {
      assertThat(p.getCapabilitySetId()).isNotNull();
      var name = p.getPermissionName();
      var capabilitySet = capSetByPermission.get(name);
      assertThat(capabilitySet).isNotNull();
      assertThat(p.getCapabilitySetId()).isEqualTo(capabilitySet.getId());
    };
  }

  private static ThrowingConsumer<LoadablePermission> capabilityAssignedToOneFrom(
    Map<String, Capability> capSetByPermission) {
    return p -> {
      assertThat(p.getCapabilityId()).isNotNull();
      var name = p.getPermissionName();
      var capability = capSetByPermission.get(name);
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
    assertThat(loadableRoles).hasSize(1);
    return loadableRoles.getFirst();
  }

  private static Map<String, CapabilitySet> getExistingCapabilitySets() throws Exception {
    var mvcResult = doGet(get("/capability-sets")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .queryParam("limit", "100"))
      .andReturn();
    var capabilitySets = parseResponse(mvcResult, CapabilitySets.class).getCapabilitySets();
    assertThat(capabilitySets).isNotEmpty();

    return capabilitySets.stream().collect(toMap(CapabilitySet::getPermission, identity()));
  }

  private static Map<String, Capability> getExistingCapabilities() throws Exception {
    var mvcResult = doGet(get("/capabilities")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)
      .queryParam("limit", "100"))
      .andReturn();
    var capabilities = parseResponse(mvcResult, Capabilities.class).getCapabilities();
    assertThat(capabilities).isNotEmpty();

    return capabilities.stream().collect(toMap(Capability::getPermission, identity()));
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(ONE_MINUTE).pollInterval(TWO_HUNDRED_MILLISECONDS);
  }
}
