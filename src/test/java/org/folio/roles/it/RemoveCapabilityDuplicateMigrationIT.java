package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.test.TestUtils.parseResponse;
import static org.springframework.cache.interceptor.SimpleKey.EMPTY;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.folio.common.utils.permission.model.PermissionAction;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.common.utils.permission.model.PermissionType;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.PermissionsUser;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleCapabilitiesRequest;
import org.folio.roles.domain.dto.UserCapabilitiesRequest;
import org.folio.roles.domain.dto.UserCapabilitySetsRequest;
import org.folio.roles.domain.dto.UserRolesRequest;
import org.folio.roles.integration.kafka.model.ResourceEvent;
import org.folio.roles.service.migration.CapabilityDuplicateMigrationService;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.jdbc.Sql;

@Log4j2
@IntegrationTest
@Sql(scripts = "/sql/capability-duplicates-cleanup.sql", executionPhase = AFTER_TEST_METHOD)
class RemoveCapabilityDuplicateMigrationIT extends BaseIntegrationTest {

  private static final String OLD_CAPABILITY_NAME = "foo_test_migration_test.view";
  private static final String NEW_CAPABILITY_NAME = "foo_migration_test_get.execute";
  private static final String PERMISSION = "foo.migration.test.get";
  private static final UUID TEST_USER_ID = UUID.fromString("43ecf012-1e1b-445a-9aa4-092e84b60805");

  @Autowired private KafkaTemplate<String, ResourceEvent> kafkaTemplate;
  @Autowired private Keycloak keycloak;
  @Autowired private CacheManager cacheManager;
  @Autowired private CapabilityDuplicateMigrationService capabilityDuplicateMigrationService;

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
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_capabilityLinkedToRole() {
    createDuplicateCapabilities();
    var oldCapId = getCapabilityIdByName(OLD_CAPABILITY_NAME);

    var roleId = createRole("test-migration-role-1");
    assignCapabilityToRole(roleId, oldCapId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilityDeleted(OLD_CAPABILITY_NAME);
    assertCapabilityExists(NEW_CAPABILITY_NAME);

    var newCapId = getCapabilityIdByName(NEW_CAPABILITY_NAME);
    var roleCapabilities = getRoleCapabilityIds(roleId);
    assertThat(roleCapabilities)
      .contains(newCapId)
      .doesNotContain(oldCapId);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_capabilityWithUserHavingAccessViaRole() {
    createDuplicateCapabilities();
    var oldCapId = getCapabilityIdByName(OLD_CAPABILITY_NAME);

    var roleId = createRole("test-migration-role-3");
    assignCapabilityToRole(roleId, oldCapId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilityDeleted(OLD_CAPABILITY_NAME);
    assertCapabilityExists(NEW_CAPABILITY_NAME);

    var newCapId = getCapabilityIdByName(NEW_CAPABILITY_NAME);
    var roleCapabilities = getRoleCapabilityIds(roleId);
    assertThat(roleCapabilities).contains(newCapId);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_bothCapabilityAndCapabilitySetWithMultipleReferences() {
    createDuplicateCapabilities();
    var oldCapId = getCapabilityIdByName(OLD_CAPABILITY_NAME);
    var oldCapSetId = getCapabilitySetIdByName(OLD_CAPABILITY_NAME);

    var roleId = createRole("test-migration-role-5");
    assignCapabilityToRole(roleId, oldCapId);

    assignCapabilitySetToUser(TEST_USER_ID, oldCapSetId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    var newCapId = getCapabilityIdByName(NEW_CAPABILITY_NAME);
    var roleCapabilities = getRoleCapabilityIds(roleId);
    assertThat(roleCapabilities).contains(newCapId);

    var newCapSetId = getCapabilitySetIdByName(NEW_CAPABILITY_NAME);
    var userCapabilitySets = getUserCapabilitySetIds(TEST_USER_ID);
    assertThat(userCapabilitySets).contains(newCapSetId);

    assertCapabilityDeleted(OLD_CAPABILITY_NAME);
    assertCapabilityExists(NEW_CAPABILITY_NAME);
    assertCapabilitySetDeleted(OLD_CAPABILITY_NAME);
    assertCapabilitySetExists(NEW_CAPABILITY_NAME);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_duplicateAssignmentsRoleAlreadyHasNew() {
    createDuplicateCapabilities();
    var oldCapId = getCapabilityIdByName(OLD_CAPABILITY_NAME);

    var roleId = createRole("test-migration-role-6");
    assignCapabilityToRole(roleId, oldCapId);

    var newCapId = getCapabilityIdByName(NEW_CAPABILITY_NAME);
    assignCapabilityToRole(roleId, newCapId);

    var capabilitiesBeforeMigration = getRoleCapabilityIds(roleId);
    assertThat(capabilitiesBeforeMigration).containsExactlyInAnyOrder(oldCapId, newCapId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilityDeleted(OLD_CAPABILITY_NAME);
    assertCapabilityExists(NEW_CAPABILITY_NAME);

    var capabilitiesAfterMigration = getRoleCapabilityIds(roleId);
    assertThat(capabilitiesAfterMigration)
      .containsExactly(newCapId)
      .hasSize(1);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_idempotentRunMultipleTimes() {
    createDuplicateCapabilities();
    var oldCapId = getCapabilityIdByName(OLD_CAPABILITY_NAME);

    var roleId = createRole("test-migration-role-7");
    assignCapabilityToRole(roleId, oldCapId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilityDeleted(OLD_CAPABILITY_NAME);
    assertCapabilityExists(NEW_CAPABILITY_NAME);
    var newCapId = getCapabilityIdByName(NEW_CAPABILITY_NAME);
    var capabilitiesAfterFirstRun = getRoleCapabilityIds(roleId);
    assertThat(capabilitiesAfterFirstRun).contains(newCapId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    var capabilitiesAfterSecondRun = getRoleCapabilityIds(roleId);
    assertThat(capabilitiesAfterSecondRun).isEqualTo(capabilitiesAfterFirstRun);
  }

  @Test
  void migrate_positive_skipWhenOldCapabilityNotFound() {
    sendKafkaEvent("event2-capability-without-override.json");

    await().untilAsserted(() ->
      assertCapabilityExists(NEW_CAPABILITY_NAME)
    );

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilityExists(NEW_CAPABILITY_NAME);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_loadableRoleWithOldCapability() {
    createDuplicateCapabilities();
    var oldCapId = getCapabilityIdByName(OLD_CAPABILITY_NAME);

    var loadableRoleId = createLoadableRole("test-loadable-role-migration");

    assignCapabilityToLoadableRole(loadableRoleId, PERMISSION, oldCapId);

    var loadableCapabilityId = getLoadableRoleCapabilityId(loadableRoleId, PERMISSION);
    assertThat(loadableCapabilityId).isEqualTo(oldCapId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilityDeleted(OLD_CAPABILITY_NAME);
    assertCapabilityExists(NEW_CAPABILITY_NAME);

    var newCapId = getCapabilityIdByName(NEW_CAPABILITY_NAME);
    var updatedCapabilityId = getLoadableRoleCapabilityId(loadableRoleId, PERMISSION);
    assertThat(updatedCapabilityId).isEqualTo(newCapId);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_loadableRoleWithOldCapabilitySet() {
    createDuplicateCapabilities();
    var oldCapSetId = getCapabilitySetIdByName(OLD_CAPABILITY_NAME);

    var loadableRoleId = createLoadableRole("test-loadable-role-capset-migration");

    assignCapabilitySetToLoadableRole(loadableRoleId, PERMISSION, oldCapSetId);

    var loadableCapSetId = getLoadableRoleCapabilitySetId(loadableRoleId, PERMISSION);
    assertThat(loadableCapSetId).isEqualTo(oldCapSetId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilitySetDeleted(OLD_CAPABILITY_NAME);
    assertCapabilitySetExists(NEW_CAPABILITY_NAME);

    var newCapSetId = getCapabilitySetIdByName(NEW_CAPABILITY_NAME);
    var updatedCapSetId = getLoadableRoleCapabilitySetId(loadableRoleId, PERMISSION);
    assertThat(updatedCapSetId).isEqualTo(newCapSetId);
  }

  @Test
  @KeycloakRealms("classpath:json/keycloak/test-realm-for-assignment.json")
  void migrate_positive_loadableRoleDuplicateAssignments() {
    createDuplicateCapabilities();
    var oldCapId = getCapabilityIdByName(OLD_CAPABILITY_NAME);
    var newCapId = getCapabilityIdByName(NEW_CAPABILITY_NAME);

    var loadableRoleId = createLoadableRole("test-loadable-role-duplicate");

    assignCapabilityToLoadableRole(loadableRoleId, PERMISSION, oldCapId);
    assignCapabilityToLoadableRole(loadableRoleId, PERMISSION + ".extra", newCapId);

    executeMigration(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    assertCapabilityDeleted(OLD_CAPABILITY_NAME);
    assertCapabilityExists(NEW_CAPABILITY_NAME);

    var remainingCapabilities = getLoadableRoleCapabilities(loadableRoleId);
    assertThat(remainingCapabilities)
      .containsEntry(PERMISSION, newCapId)
      .doesNotContainEntry(PERMISSION, oldCapId);
  }

  @SneakyThrows
  private void createDuplicateCapabilities() {
    // Prepare mappings for first event
    var mappingWithOverride = new HashMap<>();
    mappingWithOverride.put("foo.migration.test.get", new PermissionData(
      "Foo Test Migration Test",
      PermissionType.DATA,
      PermissionAction.VIEW,
      "foo.migration.test.get"
    ));
    var cache = cacheManager.getCache("permission-mappings");
    if (cache != null) {
      cache.clear();
    }

    // Event 1: Populate cache with override mapping using correct cache key
    if (cache != null) {
      cache.put(EMPTY, mappingWithOverride);
    }

    sendKafkaEvent("event1-capability-with-override.json");

    await().untilAsserted(() ->
      assertCapabilityExists(OLD_CAPABILITY_NAME)
    );

    // Event 2: Clear cache to simulate missing override
    if (cache != null) {
      cache.clear();
      cache.put(EMPTY, Map.of());
    }

    sendKafkaEvent("event2-capability-without-override.json");
    await().untilAsserted(() -> {
        assertCapabilityExists(OLD_CAPABILITY_NAME);
        assertCapabilityExists(NEW_CAPABILITY_NAME);
      }
    );
  }

  private void sendKafkaEvent(String eventFileName) {
    var event = readValue("json/kafka-events/capability-duplication/" + eventFileName, ResourceEvent.class);
    kafkaTemplate.send(FOLIO_IT_CAPABILITIES_TOPIC, event);
  }

  @SneakyThrows
  private void executeMigration(String oldCapabilityName, String newCapabilityName) {
    var headers = Map.<String, Collection<String>>of(XOkapiHeaders.TENANT, List.of(TENANT_ID));
    var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), headers);
    try (var ignored = new FolioExecutionContextSetter(context)) {
      capabilityDuplicateMigrationService.migrate(oldCapabilityName, newCapabilityName);
    }
  }

  @SneakyThrows
  private static UUID getCapabilityIdByName(String name) {
    var capability =
      parseResponse(doGet("/capabilities?query=name==" + name).andReturn(), Capabilities.class).getCapabilities();
    return capability.getFirst().getId();
  }

  @SneakyThrows
  private static UUID getCapabilitySetIdByName(String name) {
    var capabilitySets =
      parseResponse(doGet("/capability-sets?query=name==" + name).andReturn(),
        org.folio.roles.domain.dto.CapabilitySets.class).getCapabilitySets();
    return capabilitySets.getFirst().getId();
  }

  @SneakyThrows
  private static List<UUID> getRoleCapabilityIds(UUID roleId) {
    var result = doGet("/roles/{id}/capabilities", roleId).andReturn();
    var roleCapabilities = parseResponse(result, Capabilities.class);
    return roleCapabilities.getCapabilities().stream()
      .map(org.folio.roles.domain.dto.Capability::getId)
      .toList();
  }

  @SneakyThrows
  private static List<UUID> getUserCapabilityIds(UUID userId) {
    var result = doGet("/users/{id}/capabilities", userId).andReturn();
    var userCapabilities = parseResponse(result, org.folio.roles.domain.dto.Capabilities.class);
    return userCapabilities.getCapabilities().stream()
      .map(org.folio.roles.domain.dto.Capability::getId)
      .toList();
  }

  @SneakyThrows
  private static List<UUID> getRoleCapabilitySetIds(UUID roleId) {
    var result = doGet("/roles/{id}/capability-sets", roleId).andReturn();
    var roleCapabilitySets = parseResponse(result, org.folio.roles.domain.dto.CapabilitySets.class);
    return roleCapabilitySets.getCapabilitySets().stream()
      .map(org.folio.roles.domain.dto.CapabilitySet::getId)
      .toList();
  }

  @SneakyThrows
  private static List<UUID> getUserCapabilitySetIds(UUID userId) {
    var result = doGet("/users/{id}/capability-sets", userId).andReturn();
    var userCapabilitySets = parseResponse(result, org.folio.roles.domain.dto.CapabilitySets.class);
    return userCapabilitySets.getCapabilitySets().stream()
      .map(org.folio.roles.domain.dto.CapabilitySet::getId)
      .toList();
  }

  @SneakyThrows
  private static void assertUserHasPermission(UUID userId) {
    var response = doGet("/permissions/users/{id}", userId).andReturn();
    var permissionsUser = parseResponse(response, PermissionsUser.class);
    assertThat(permissionsUser.getPermissions()).contains(PERMISSION);
  }

  @SneakyThrows
  private static UUID createRole(String roleName) {
    var role = new Role().name(roleName).description("Test role for migration");
    return parseResponse(doPost("/roles", role).andReturn(), Role.class).getId();
  }

  @SneakyThrows
  private static void assignCapabilityToRole(UUID roleId, UUID capabilityId) {
    var request = new RoleCapabilitiesRequest()
      .roleId(roleId)
      .capabilityIds(List.of(capabilityId));
    doPost("/roles/capabilities", request);
  }

  @SneakyThrows
  private static void assignCapabilityToUser(UUID userId, UUID capabilityId) {
    var request = new UserCapabilitiesRequest().userId(userId).capabilityIds(List.of(capabilityId));
    doPost("/users/capabilities", request);
  }

  @SneakyThrows
  private static void assignCapabilitySetToUser(UUID userId, UUID capabilitySetId) {
    var request = new UserCapabilitySetsRequest().userId(userId).capabilitySetIds(List.of(capabilitySetId));
    doPost("/users/capability-sets", request);
  }

  @SneakyThrows
  private static void assignRoleToUser(UUID userId, UUID roleId) {
    var request = new UserRolesRequest().roleIds(List.of(roleId));
    attemptPost("/users/{id}/roles", request, userId).andExpect(status().isOk());
  }

  @SneakyThrows
  private static UUID createLoadableRole(String roleName) {
    var role = new LoadableRole()
      .name(roleName)
      .description("Test loadable role for migration")
      .permissions(List.of());

    var result = doPut("/loadable-roles", role).andReturn();
    return parseResponse(result, LoadableRole.class).getId();
  }

  @SneakyThrows
  private static void assignCapabilityToLoadableRole(UUID roleId, String permissionName, UUID capabilityId) {
    var role = parseResponse(doGet("/loadable-roles?query=id==" + roleId).andReturn(),
      org.folio.roles.domain.dto.LoadableRoles.class).getLoadableRoles().getFirst();

    var permissions = new java.util.ArrayList<>(role.getPermissions() != null ? role.getPermissions() : List.of());
    permissions.add(new LoadablePermission().permissionName(permissionName).capabilityId(capabilityId));

    role.permissions(permissions);
    doPut("/loadable-roles", role);
  }

  @SneakyThrows
  private static void assignCapabilitySetToLoadableRole(UUID roleId, String permissionName, UUID capabilitySetId) {
    var role = parseResponse(doGet("/loadable-roles?query=id==" + roleId).andReturn(),
      org.folio.roles.domain.dto.LoadableRoles.class).getLoadableRoles().getFirst();

    var permissions = new java.util.ArrayList<>(role.getPermissions() != null ? role.getPermissions() : List.of());
    permissions.add(new LoadablePermission().permissionName(permissionName).capabilitySetId(capabilitySetId));

    role.permissions(permissions);
    doPut("/loadable-roles", role);
  }

  @SneakyThrows
  private static UUID getLoadableRoleCapabilityId(UUID roleId, String permissionName) {
    var loadableRole = getLoadableRole(roleId);
    return loadableRole.getPermissions().stream()
      .filter(p -> p.getPermissionName().equals(permissionName))
      .findFirst()
      .map(LoadablePermission::getCapabilityId)
      .orElseThrow(() -> new IllegalStateException("Permission not found: " + permissionName));
  }

  @SneakyThrows
  private static UUID getLoadableRoleCapabilitySetId(UUID roleId, String permissionName) {
    var loadableRole = getLoadableRole(roleId);
    return loadableRole.getPermissions().stream()
      .filter(p -> p.getPermissionName().equals(permissionName))
      .findFirst()
      .map(LoadablePermission::getCapabilitySetId)
      .orElseThrow(() -> new IllegalStateException("Permission not found: " + permissionName));
  }

  @SneakyThrows
  private static Map<String, UUID> getLoadableRoleCapabilities(UUID roleId) {
    var loadableRole = getLoadableRole(roleId);
    return loadableRole.getPermissions().stream()
      .filter(p -> p.getCapabilityId() != null)
      .collect(Collectors.toMap(
        LoadablePermission::getPermissionName,
        LoadablePermission::getCapabilityId
      ));
  }

  @SneakyThrows
  private static LoadableRole getLoadableRole(UUID roleId) {
    var response = doGet("/loadable-roles?query=id==" + roleId).andReturn();
    return parseResponse(response, org.folio.roles.domain.dto.LoadableRoles.class)
      .getLoadableRoles()
      .getFirst();
  }

  @SneakyThrows
  private static void assertCapabilityExists(String capabilityName) {
    var response = doGet("/capabilities?query=name==" + capabilityName).andReturn();
    var capabilities = parseResponse(response, Capabilities.class).getCapabilities();
    assertThat(capabilities)
      .hasSize(1)
      .first()
      .satisfies(cap -> assertThat(cap.getName()).isEqualTo(capabilityName));
  }

  @SneakyThrows
  private static void assertCapabilityDeleted(String capabilityName) {
    var response = doGet("/capabilities?query=name==" + capabilityName).andReturn();
    var capabilities = parseResponse(response, Capabilities.class).getCapabilities();
    assertThat(capabilities).isEmpty();
  }

  @SneakyThrows
  private static void assertCapabilitySetDeleted(String capabilitySetName) {
    var response = doGet("/capability-sets?query=name==" + capabilitySetName).andReturn();
    var capabilitySets = parseResponse(response, org.folio.roles.domain.dto.CapabilitySets.class).getCapabilitySets();
    assertThat(capabilitySets).isEmpty();
  }

  @SneakyThrows
  private static void assertCapabilitySetExists(String capabilitySetName) {
    var response = doGet("/capability-sets?query=name==" + capabilitySetName).andReturn();
    var capabilitySets = parseResponse(response, org.folio.roles.domain.dto.CapabilitySets.class).getCapabilitySets();
    assertThat(capabilitySets)
      .hasSize(1)
      .first()
      .satisfies(capabilitySet -> assertThat(capabilitySet.getName()).isEqualTo(capabilitySetName));
  }

  private static ConditionFactory await() {
    return Awaitility.await().atMost(ONE_MINUTE).pollInterval(FIVE_HUNDRED_MILLISECONDS);
  }
}
