package org.folio.roles.it;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.TWO_HUNDRED_MILLISECONDS;
import static org.folio.roles.service.role.RolePolicyNameProvider.getPermissionNameGenerator;
import static org.folio.roles.service.role.RolePolicyNameProvider.getPolicyName;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID_HEADER;
import static org.folio.roles.utils.TestValues.readValue;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.folio.test.TestUtils.parseResponse;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.ThrowingConsumer;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.folio.common.domain.model.error.ErrorResponse;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.roles.KeycloakTestClient;
import org.folio.roles.base.BaseIntegrationTest;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.integration.kafka.KafkaMessageListener;
import org.folio.roles.repository.LoadableRoleRepository;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.RoleCapabilityServiceImpl;
import org.folio.roles.service.loadablerole.LoadablePermissionService;
import org.folio.roles.service.loadablerole.LoadableRoleCapabilityAssignmentHelper;
import org.folio.roles.service.loadablerole.LoadableRoleCapabilityAssignmentProcessor;
import org.folio.roles.service.loadablerole.LoadableRoleService;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.policy.PolicyEntityService;
import org.folio.roles.service.role.RoleEntityService;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.keycloak.admin.client.Keycloak;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.jdbc.JdbcTestUtils;

@Log4j2
@IntegrationTest
@Import(KeycloakTestClient.class)
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
  @Autowired private KeycloakTestClient keycloakTestClient;
  @Autowired private KafkaMessageListener kafkaMessageListener;

  @MockitoSpyBean private LoadableRoleCapabilityAssignmentHelper assignmentHelper;
  @MockitoSpyBean private LoadableRoleCapabilityAssignmentProcessor assignmentProcessor;
  @MockitoSpyBean private RolePermissionService rolePermissionService;
  @MockitoSpyBean private PolicyEntityService policyEntityService;
  @MockitoSpyBean private RoleCapabilityServiceImpl roleCapabilityService;
  @Autowired private CapabilityService capabilityService;
  @Autowired private LoadableRoleService loadableRoleService;
  @Autowired private LoadablePermissionService loadablePermissionService;
  @Autowired private EntityManager entityManager;
  @MockitoSpyBean private RoleEntityService roleEntityService;
  @MockitoSpyBean private LoadableRoleRepository loadableRoleRepository;

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
  @Sql(scripts = "classpath:/sql/populate-role-loadable-with-be-technical-capabilities.sql")
  void assignCapabilities_positive_includingTechnical() throws Exception {
    sendCapabilityEvent("json/kafka-events/be-technical-capability-event.json");

    await().untilAsserted(() -> {
      int count = unassignedCapabilityCountForPermissionLike("technical.item.get");
      assertThat(count).isZero();
    });

    var capabilitiesByPermission = getExistingCapabilities();

    verifyAssignedCapabilities(CIRC_MANAGER_ROLE_NAME,
      role -> selectPermissionsLike(role, "technical.item.get"),
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
      .andReturn();

    var createdRole = parseResponse(mvcResult, LoadableRole.class);
    assertThat(createdRole.getName()).isEqualTo(role.getName());
    assertThat(createdRole.getDescription()).isEqualTo(role.getDescription());
    assertThat(createdRole.getPermissions()).hasSize(1);
    assertThat(createdRole.getPermissions().getFirst().getPermissionName()).isEqualTo("test.permission");
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void upsertLoadableRole_negative_roleNameContainsForbiddenCharacter() throws Exception {
    var invalidName = "Circulation/Manager";
    var role = new LoadableRole()
      .name(invalidName)
      .description("Role with a forbidden character in the name")
      .permissions(List.of(new LoadablePermission().permissionName("test.permission")));

    attemptPut("/loadable-roles", role)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.errors[0].code", is("validation_error")))
      .andExpect(jsonPath("$.errors[0].parameters[0].key", is("name")))
      .andExpect(jsonPath("$.errors[0].parameters[0].value", is(invalidName)));

    var loadableRoles = parseResponse(doGet(get("/loadable-roles")
      .header(TENANT, TENANT_ID)
      .header(USER_ID, USER_ID_HEADER)).andReturn(), LoadableRoles.class);

    assertThat(loadableRoles.getLoadableRoles()).noneMatch(r -> invalidName.equals(r.getName()));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void upsertLoadableRole_positive_updateExistingRole() throws Exception {
    var initialRole = new LoadableRole()
      .name("Update Test Role")
      .description("Initial description")
      .permissions(List.of(new LoadablePermission().permissionName("initial.permission")));

    var createResult = doPut("/loadable-roles", initialRole)
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

    doPut("/loadable-roles", role);

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
  void upsertLoadableRole_positive_handlesCapabilityAssignmentRaceCondition() throws Exception {
    var permissionName = "notes.collection.get";
    var roleName = "Race Condition Role";
    var role = new LoadableRole()
      .name(roleName)
      .description("Role used to reproduce the capability assignment race")
      .permissions(List.of(new LoadablePermission().permissionName(permissionName)));

    var helperCompletedInsideTransaction = new CountDownLatch(1);
    var allowLoadableRoleCommit = new CountDownLatch(1);

    doAnswer(invocation -> {
      var result = invocation.callRealMethod();
      helperCompletedInsideTransaction.countDown();
      assertThat(allowLoadableRoleCommit.await(10, TimeUnit.SECONDS)).isTrue();
      return result;
    }).when(assignmentHelper).assignCapabilitiesAndSetsForPermissions(anyCollection());

    try (var executor = Executors.newSingleThreadExecutor()) {
      final var upsertFuture = executor.submit(() -> {
        doPut("/loadable-roles", role);
        return null;
      });

      assertThat(helperCompletedInsideTransaction.await(10, TimeUnit.SECONDS)).isTrue();

      var capabilityEvent = readValue("json/kafka-events/be-notes-capability-event.json", ResourceEvent.class);
      kafkaMessageListener.handleCapabilityEvent(capabilityEvent);

      allowLoadableRoleCommit.countDown();
      upsertFuture.get(10, TimeUnit.SECONDS);
    }

    assertThat(unassignedCapabilityCountForPermissionLike(permissionName)).isZero();

    var fetchedRole = getLoadableRoleByName(roleName);
    var matchingPermission = fetchedRole.getPermissions().stream()
      .filter(permission -> permissionName.equals(permission.getPermissionName()))
      .findFirst()
      .orElseThrow();
    assertThat(matchingPermission.getCapabilityId()).isNotNull();

    var roleCapabilities = parseResponse(doGet("/roles/{id}/capabilities", fetchedRole.getId()).andReturn(),
      Capabilities.class);
    assertThat(roleCapabilities.getCapabilities())
      .extracting(Capability::getPermission)
      .contains(permissionName);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void upsertLoadableRole_concurrentAssignment_assignsWholeBatch(boolean policyRace) throws Exception {
    var roleName = "Concurrent Assignment Role";
    var competingPermission = "notes.item.get";
    var role = new LoadableRole().name(roleName).description("Concurrent assignment")
      .permissions(List.of(new LoadablePermission().permissionName("notes.collection.get"),
        new LoadablePermission().permissionName(competingPermission)));
    var initialAssignmentDone = new CountDownLatch(1);
    var allowRoleCommit = new CountDownLatch(1);
    var roleCommitted = new CountDownLatch(1);
    var allowAssignment = new CountDownLatch(1);
    var assignmentCalls = new AtomicInteger();
    doAnswer(invocation -> {
      if (assignmentCalls.incrementAndGet() == 1) {
        var result = invocation.callRealMethod();
        initialAssignmentDone.countDown();
        assertThat(allowRoleCommit.await(30, TimeUnit.SECONDS)).isTrue();
        return result;
      }
      roleCommitted.countDown();
      assertThat(allowAssignment.await(30, TimeUnit.SECONDS)).isTrue();
      return invocation.callRealMethod();
    }).when(assignmentHelper).assignCapabilitiesAndSetsForPermissions(anyCollection());

    var assignmentPaused = new CountDownLatch(1);
    var finishAssignment = new CountDownLatch(1);
    var firstAssignment = new AtomicBoolean(true);
    Answer<Object> pauseAssignment = invocation -> {
      var result = invocation.callRealMethod();
      if (firstAssignment.compareAndSet(true, false)) {
        assignmentPaused.countDown();
        assertThat(finishAssignment.await(30, TimeUnit.SECONDS)).isTrue();
      }
      return result;
    };

    try (var executor = Executors.newFixedThreadPool(2)) {
      var request = executor.submit(() -> {
        doPut("/loadable-roles", role);
        return null;
      });
      try {
        assertThat(initialAssignmentDone.await(30, TimeUnit.SECONDS)).isTrue();
        kafkaMessageListener.handleCapabilityEvent(
          readValue("json/kafka-events/be-notes-capability-event.json", ResourceEvent.class));
        allowRoleCommit.countDown();
        assertThat(roleCommitted.await(30, TimeUnit.SECONDS)).isTrue();
        var context = tenantExecutionContext();
        Capability competingCapability;
        try (var ignored = new FolioExecutionContextSetter(context)) {
          competingCapability = capabilityService.findByPermissionNames(List.of(competingPermission)).getFirst();
          if (!policyRace) {
            rolePermissionService.createPermissions(getLoadableRoleByName(roleName).getId(),
              competingCapability.getEndpoints());
          }
        }
        if (policyRace) {
          doAnswer(pauseAssignment).when(policyEntityService).findByName(any());
        } else {
          doAnswer(pauseAssignment).when(roleCapabilityService).create(any(UUID.class), anyList(), eq(true));
        }
        allowAssignment.countDown();
        assertThat(assignmentPaused.await(30, TimeUnit.SECONDS)).isTrue();
        var competingAssignment = executor.submit(() -> {
          try (var ignored = new FolioExecutionContextSetter(context)) {
            assignmentProcessor.handleCapabilitiesCreatedEvent(
              (CapabilityEvent) CapabilityEvent.created(competingCapability).withContext(context));
          }
        });
        // On the old code the competitor commits first; with serialization it waits for the role lock.
        await().until(() -> competingAssignment.isDone() || roleAssignmentWaitingForLock());
        finishAssignment.countDown();
        competingAssignment.get(30, TimeUnit.SECONDS);
        request.get(30, TimeUnit.SECONDS);
      } finally {
        allowRoleCommit.countDown();
        allowAssignment.countDown();
        finishAssignment.countDown();
      }
    }

    assertThat(firstAssignment).isFalse();
    assertThat(unassignedCapabilityCountForPermissionLike("note")).isZero();
    var capabilities = parseResponse(
      doGet("/roles/{id}/capabilities", getLoadableRoleByName(roleName).getId()).andReturn(), Capabilities.class);
    assertThat(capabilities.getCapabilities()).extracting(Capability::getPermission)
      .containsExactlyInAnyOrder("notes.collection.get", competingPermission);
    var roleId = getLoadableRoleByName(roleName).getId();
    assertThat(keycloakTestClient.getPolicyNames())
      .contains(getPolicyName(roleId));
    var permissionName = getPermissionNameGenerator(roleId);
    assertThat(keycloakTestClient.getPermissionNames()).containsAll(capabilities.getCapabilities().stream()
      .flatMap(capability -> capability.getEndpoints().stream()).map(permissionName).toList());
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void saveAll_reversedRoleOrderAndConcurrentAssignment_completesWithoutDeadlock() throws Exception {
    var firstRole = new LoadableRole().name("First Bulk Role").description("Initial").type(RoleType.DEFAULT)
      .permissions(List.of(new LoadablePermission().permissionName("existing.permission")));
    var secondRole = new LoadableRole().name("Second Bulk Role").description("Initial").type(RoleType.DEFAULT)
      .permissions(List.of(new LoadablePermission().permissionName("existing.permission")));
    var context = tenantExecutionContext();
    try (var ignored = new FolioExecutionContextSetter(context)) {
      loadableRoleService.saveAll(List.of(firstRole, secondRole));
    }
    var roles = List.of(getLoadableRoleByName(firstRole.getName()), getLoadableRoleByName(secondRole.getName()));
    final var keys = roles.stream().map(role -> LoadablePermissionKey.of(role.getId(), "existing.permission")).toList();
    roles.forEach(role -> role.description("Updated")
      .addPermissionsItem(new LoadablePermission().permissionName("new.permission")));

    doAnswer(invocation -> entityManager.createQuery("""
      select role from LoadableRoleEntity role
      where role.type = :type and role.loadedFromFile = true
      """, LoadableRoleEntity.class).setParameter("type", EntityRoleType.DEFAULT).getResultStream()
      .sorted(Comparator.comparing(LoadableRoleEntity::getId).reversed()))
      .when(loadableRoleRepository).findAllByTypeAndLoadedFromFile(EntityRoleType.DEFAULT, true);

    var firstRoleLocked = new CountDownLatch(1);
    var finishBulkUpdate = new CountDownLatch(1);
    var firstLock = new AtomicBoolean(true);
    doAnswer(invocation -> {
      invocation.callRealMethod();
      if (firstLock.compareAndSet(true, false)) {
        firstRoleLocked.countDown();
        assertThat(finishBulkUpdate.await(30, TimeUnit.SECONDS)).isTrue();
      }
      return null;
    }).when(roleEntityService).lockById(any(UUID.class));

    try (var executor = Executors.newFixedThreadPool(2)) {
      var bulkUpdate = executor.submit(() -> {
        try (var ignored = new FolioExecutionContextSetter(context)) {
          loadableRoleService.saveAll(roles);
        }
      });
      try {
        assertThat(firstRoleLocked.await(30, TimeUnit.SECONDS)).isTrue();
        final var assignment = executor.submit(() -> {
          try (var ignored = new FolioExecutionContextSetter(context)) {
            loadablePermissionService.assignCapabilitiesAndSets(keys);
          }
        });
        await().until(this::roleAssignmentWaitingForLock);
        finishBulkUpdate.countDown();
        bulkUpdate.get(30, TimeUnit.SECONDS);
        assignment.get(30, TimeUnit.SECONDS);
      } finally {
        finishBulkUpdate.countDown();
      }
    }

    assertThat(getLoadableRoleByName(firstRole.getName()).getDescription()).isEqualTo("Updated");
    assertThat(getLoadableRoleByName(secondRole.getName()).getDescription()).isEqualTo("Updated");
  }

  private boolean roleAssignmentWaitingForLock() {
    return Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
      select exists(select 1 from pg_stat_activity
        where datname = current_database() and wait_event_type = 'Lock'
          and lower(query) like '%for no key update%')
      """, Boolean.class));
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void handleCapabilityEvent_positive_whenOneMatchingRoleAlreadyHasCapabilityAndAnotherDoesNot() throws Exception {
    var permissionName = "notes.collection.get";
    var firstRoleName = "Initially Unresolved Role";
    var secondRoleName = "Assigned While Event Blocked Role";

    // Step 1: Create the first role having permission "notes.collection.get".
    // Because the capability does not exist for this permission, the "capabilityId" should be null for this role
    // in the DB.
    var firstRole = new LoadableRole()
      .name(firstRoleName)
      .description("Role created before capability exists")
      .permissions(List.of(new LoadablePermission().permissionName(permissionName)));

    doPut("/loadable-roles", firstRole);

    var createdFirstRole = getLoadableRoleByName(firstRoleName);
    assertThat(findPermission(createdFirstRole, permissionName).getCapabilityId()).isNull();

    // Step 2: Create the capability now. However, pause handleCapabilitiesCreatedEvent(...) after the capability
    // event reaches the listener to simulate the race condition.
    var processorEntered = new CountDownLatch(1);
    var allowProcessorToContinue = new CountDownLatch(1);

    doAnswer(invocation -> {
      processorEntered.countDown();
      assertThat(allowProcessorToContinue.await(10, TimeUnit.SECONDS)).isTrue();
      return invocation.callRealMethod();
    }).when(assignmentProcessor).handleCapabilitiesCreatedEvent(any());

    try (var executor = Executors.newSingleThreadExecutor()) {
      var capabilityEvent = readValue("json/kafka-events/be-notes-capability-event.json", ResourceEvent.class);
      final var createCapabilityFuture = executor.submit(() -> {
        kafkaMessageListener.handleCapabilityEvent(capabilityEvent);
        return null;
      });

      assertThat(processorEntered.await(10, TimeUnit.SECONDS)).isTrue();

      // Step 3: While the listener is blocked, creating another loadable role for the same permission.
      // This should resolve immediately because the capability now exists and the normal save path can see it.
      var secondRole = new LoadableRole()
        .name(secondRoleName)
        .description("Role created while capability assignment processor is blocked")
        .permissions(List.of(new LoadablePermission().permissionName(permissionName)));
      doPut("/loadable-roles", secondRole);

      var capabilityId = getExistingCapabilities().get(permissionName).getId();
      assertThat(capabilityId).isNotNull();

      var unresolvedFirstRole = getLoadableRoleByName(firstRoleName);
      var assignedSecondRole = getLoadableRoleByName(secondRoleName);

      // Before unblocking the listener, only the second role should be assigned.
      assertThat(findPermission(unresolvedFirstRole, permissionName).getCapabilityId()).isNull();
      assertThat(findPermission(assignedSecondRole, permissionName).getCapabilityId()).isEqualTo(capabilityId);
      assertThat(unassignedCapabilityCountForRoleAndPermission(unresolvedFirstRole.getId(), permissionName))
        .isEqualTo(1);
      assertThat(roleCapabilityCount(unresolvedFirstRole.getId(), capabilityId)).isZero();
      assertThat(roleCapabilityCount(assignedSecondRole.getId(), capabilityId)).isEqualTo(1);

      // Step 4: Now, unpause the listener.
      // Once the listener continues, it should repair the first role without disturbing the second one.
      allowProcessorToContinue.countDown();
      createCapabilityFuture.get(10, TimeUnit.SECONDS);

      await().untilAsserted(() -> {
        var resolvedFirstRole = getLoadableRoleByName(firstRoleName);
        var resolvedSecondRole = getLoadableRoleByName(secondRoleName);

        assertThat(findPermission(resolvedFirstRole, permissionName).getCapabilityId()).isEqualTo(capabilityId);
        assertThat(findPermission(resolvedSecondRole, permissionName).getCapabilityId()).isEqualTo(capabilityId);
        assertThat(roleCapabilityCount(resolvedFirstRole.getId(), capabilityId)).isEqualTo(1);
        assertThat(roleCapabilityCount(resolvedSecondRole.getId(), capabilityId)).isEqualTo(1);
      });
    }
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void handleCapabilityEvent_existingConcurrentAssignment_assignsBothRoles() throws Exception {
    var permissionName = "notes.collection.get";
    var firstRoleName = "Unique Violation First Role";

    // Step 1: Create two roles having permission "notes.collection.get" before the capability exists,
    // so both have unresolved (null) capability ids, mirroring parallel entitlement.
    doPut("/loadable-roles", new LoadableRole()
      .name(firstRoleName)
      .description("First role competing for the capability")
      .permissions(List.of(new LoadablePermission().permissionName(permissionName))));

    var secondRoleName = "Unique Violation Second Role";
    doPut("/loadable-roles", new LoadableRole()
      .name(secondRoleName)
      .description("Second role competing for the capability")
      .permissions(List.of(new LoadablePermission().permissionName(permissionName))));

    // Commit a concurrent assignment before Hibernate merge checks whether the row exists.
    var conflictInserted = new AtomicBoolean();
    doAnswer(invocation -> {
      if (conflictInserted.compareAndSet(false, true)) {
        UUID roleId = invocation.getArgument(0);
        // a separate thread gets its own auto-committed connection instead of joining the event transaction
        var conflictingInsert = new Thread(() -> {
          var capabilityId = jdbcTemplate.queryForObject(
            "select id from test_mod_roles_keycloak.capability where folio_permission = ?", UUID.class,
            permissionName);
          jdbcTemplate.update("insert into test_mod_roles_keycloak.role_capability (role_id, capability_id) "
            + "values (?, ?)", roleId, capabilityId);
        });
        conflictingInsert.start();
        conflictingInsert.join();
      }
      return invocation.callRealMethod();
    }).when(rolePermissionService).createPermissions(any(), anyList());

    // Hibernate sees the committed row and completes both assignments without a constraint violation.
    sendCapabilityEvent("json/kafka-events/be-notes-capability-event.json");

    await().untilAsserted(() -> {
      var capabilityId = getExistingCapabilities().get(permissionName).getId();
      var firstRole = getLoadableRoleByName(firstRoleName);
      var secondRole = getLoadableRoleByName(secondRoleName);

      assertThat(findPermission(firstRole, permissionName).getCapabilityId()).isEqualTo(capabilityId);
      assertThat(findPermission(secondRole, permissionName).getCapabilityId()).isEqualTo(capabilityId);
      assertThat(roleCapabilityCount(firstRole.getId(), capabilityId)).isEqualTo(1);
      assertThat(roleCapabilityCount(secondRole.getId(), capabilityId)).isEqualTo(1);
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

    doPut("/loadable-roles", role);

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

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void updateRole_negative_defaultRoleCannotBeChangedViaRolesApi() throws Exception {
    var role = new LoadableRole()
      .name("Default Role")
      .description("This is the default role")
      .permissions(List.of(new LoadablePermission().permissionName("default.permission")));

    doPut("/loadable-roles", role);

    var defaultRole = getLoadableRoleByName("Default Role");

    var request = Map.of("name", "Updated Default Role");

    var mvcResult = attemptPut("/roles/{id}", request, defaultRole.getId())
      .andExpect(status().isBadRequest())
      .andReturn();

    var errorResponse = parseResponse(mvcResult, ErrorResponse.class);
    assertThat(errorResponse.getErrors().getFirst().getMessage())
      .isEqualTo("Default role cannot be created, updated or deleted via roles API.");
  }

  @Test
  @KeycloakRealms("/json/keycloak/role-loadable-processing-realm.json")
  void createRole_negative_defaultRoleCannotBeCreatedViaRolesApi() throws Exception {
    var role = Map.of(
      "name", "Default Role",
      "description", "This is the default role",
      "type", "DEFAULT");

    var mvcResult = attemptPost("/roles", role)
      .andExpect(status().isBadRequest())
      .andReturn();

    var errorResponse = parseResponse(mvcResult, ErrorResponse.class);
    assertThat(errorResponse.getErrors().getFirst().getMessage())
      .isEqualTo("Default role cannot be created, updated or deleted via roles API.");
  }

  private int unassignedCapabilitySetCountForPermissionLike(String permission) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, ROLE_LOADABLE_PERMISSION_TABLE,
      format("folio_permission LIKE '%%%s%%' and capability_set_id IS NULL", permission));
  }

  private int unassignedCapabilityCountForPermissionLike(String permission) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, ROLE_LOADABLE_PERMISSION_TABLE,
      format("folio_permission LIKE '%%%s%%' and capability_id IS NULL", permission));
  }

  private int unassignedCapabilityCountForRoleAndPermission(UUID roleId, String permission) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, ROLE_LOADABLE_PERMISSION_TABLE,
      format("role_loadable_id = '%s' and folio_permission = '%s' and capability_id IS NULL", roleId, permission));
  }

  private int roleCapabilityCount(UUID roleId, Object capabilityId) {
    return JdbcTestUtils.countRowsInTableWhere(jdbcTemplate, "test_mod_roles_keycloak.role_capability",
      format("role_id = '%s' and capability_id = '%s'", roleId, capabilityId));
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

  private static LoadablePermission findPermission(LoadableRole role, String permissionName) {
    return role.getPermissions().stream()
      .filter(permission -> permissionName.equals(permission.getPermissionName()))
      .findFirst()
      .orElseThrow();
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

  private static FolioExecutionContext tenantExecutionContext() {
    return new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(),
      Map.of(TENANT, List.of(TENANT_ID)));
  }
}
