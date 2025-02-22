package org.folio.roles.service.capability;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.model.event.DomainEventType.DELETE;
import static org.folio.roles.integration.kafka.model.ModuleType.MODULE;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermission;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.folio.roles.domain.model.CapabilityReplacements;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.domain.model.event.DomainEvent;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityReplacementsServiceTest {

  @InjectMocks private CapabilityReplacementsService unit;
  @Mock private FolioExecutionContext folioExecutionContext;
  @Mock private RoleCapabilityRepository roleCapabilityRepository;
  @Mock private RoleCapabilitySetRepository roleCapabilitySetRepository;
  @Mock private UserCapabilityRepository userCapabilityRepository;
  @Mock private UserCapabilitySetRepository userCapabilitySetRepository;
  @Mock private UserCapabilityService userCapabilityService;
  @Mock private UserCapabilitySetService userCapabilitySetService;
  @Mock private RoleCapabilityService roleCapabilityService;
  @Mock private RoleCapabilitySetService roleCapabilitySetService;
  @Mock private ApplicationEventPublisher applicationEventPublisher;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private CapabilitySetMapper capabilitySetMapper;
  @Mock private LoadablePermissionRepository loadablePermissionRepository;

  @Test
  void deduceReplacements_positive() {
    var testData = new CapabilityEvent();
    testData.setModuleId("fake-module");
    testData.setApplicationId("fake-application");
    testData.setModuleType(MODULE);
    testData.setResources(List.of(
      new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint1").method(GET)))
        .permission(new Permission().permissionName("new-perm.get")
          .replaces(List.of("old-perm.get"))),
      new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint2").method(GET))).permission(
        new Permission().permissionName("new-perm2.get")
          .replaces(List.of("old-perm2.get"))
          .subPermissions(List.of("A", "B"))),
      new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint3").method(GET)))
        .permission(new Permission().permissionName("new-perm3.get")
          .replaces(List.of("old-perm.get")))));

    var capabilityUuid = randomUUID();
    var capabilitySetUuid = randomUUID();
    when(capabilityService.findByPermissionNames(Set.of("old-perm.get", "old-perm2.get"))).thenReturn(
      List.of(new Capability().id(capabilityUuid).name("old-perm.view").permission("old-perm.get")));
    when(capabilitySetService.findByPermissionNames(Set.of("old-perm.get", "old-perm2.get"))).thenReturn(
      List.of(new CapabilitySet().id(capabilitySetUuid).name("old-perm-two.view").permission("old-perm2.get")));

    var role1Uuid = randomUUID();
    var role2Uuid = randomUUID();
    var role3Uuid = randomUUID();
    var userUuid = randomUUID();
    var user2Uuid = randomUUID();
    var user3Uuid = randomUUID();
    when(roleCapabilityRepository.findAllByCapabilityId(capabilityUuid)).thenReturn(
      List.of(RoleCapabilityEntity.of(role1Uuid, null)));
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(capabilitySetUuid)).thenReturn(
      List.of(RoleCapabilitySetEntity.of(role2Uuid, null), RoleCapabilitySetEntity.of(role3Uuid, null)));

    when(userCapabilityRepository.findAllByCapabilityId(capabilityUuid)).thenReturn(
      List.of(UserCapabilityEntity.of(userUuid, null)));
    when(userCapabilitySetRepository.findAllByCapabilitySetId(capabilitySetUuid)).thenReturn(
      List.of(UserCapabilitySetEntity.of(user2Uuid, null), UserCapabilitySetEntity.of(user3Uuid, null)));

    var replacements = unit.deduceReplacements(testData);
    assertThat(replacements).isPresent();
    assertThat(replacements.get().oldPermissionsToNewPermissions()).isEqualTo(
      Map.of("old-perm.get", Set.of("new-perm3.get", "new-perm.get"),
        "old-perm2.get", Set.of("new-perm2.get")));

    assertThat(replacements.get().oldRoleCapabByPermission()).isEqualTo(Map.of("old-perm.get", Set.of(role1Uuid)));
    assertThat(replacements.get().oldRoleCapabSetByPermission()).isEqualTo(
      Map.of("old-perm2.get", Set.of(role2Uuid, role3Uuid)));
    assertThat(replacements.get().oldUserCapabByPermission()).isEqualTo(Map.of("old-perm.get", Set.of(userUuid)));
    assertThat(replacements.get().oldUserCapabSetByPermission()).isEqualTo(
      Map.of("old-perm2.get", Set.of(user2Uuid, user3Uuid)));
  }

  @Test
  void deduceReplacements_positive_noReplacements() {
    var testData = new CapabilityEvent();
    testData.setModuleId("fake-module");
    testData.setApplicationId("fake-application");
    testData.setModuleType(MODULE);
    testData.setResources(List.of(new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint1").method(GET)))
        .permission(new Permission().permissionName("new-perm.get")),
      new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint2").method(GET)))
        .permission(new Permission().permissionName("new-perm2.get").subPermissions(List.of("A", "B"))),
      new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint3").method(GET)))
        .permission(new Permission().permissionName("new-perm3.get"))));

    var replacements = unit.deduceReplacements(testData);
    assertThat(replacements).isEmpty();
  }

  @Test
  void processReplacements_positive() {
    var cap1Id = randomUUID();
    var cap2Id = randomUUID();
    var capSet1Id = randomUUID();
    var capSet2Id = randomUUID();
    var roleId = randomUUID();

    when(capabilityService.findByPermissionNames(Set.of("newcap1.view", "newcapset2.view"))).thenReturn(
      List.of(capability(cap1Id, "newcap1.view")));
    when(capabilitySetService.findByPermissionNames(Set.of("newcap1.view", "newcapset2.view"))).thenReturn(
      List.of(capabilitySet(capSet2Id, "newcapset2.view")));
    when(capabilityService.findByPermissionNames(Set.of("newcap2.view", "newcapset1.view"))).thenReturn(
      List.of(capability(cap2Id, "newcap2.view")));
    when(capabilitySetService.findByPermissionNames(Set.of("newcap2.view", "newcapset1.view"))).thenReturn(
      List.of(capabilitySet(capSet1Id, "newcapset1.view")));
    when(capabilityService.findByPermissionNames(Set.of("oldcap1.view", "oldcapset2.view"))).thenReturn(
      List.of(capability(cap1Id, "oldcap1.view")));
    when(capabilitySetService.findByPermissionNames(Set.of("oldcap1.view", "oldcapset2.view"))).thenReturn(
      List.of(capabilitySet(capSet1Id, "oldcapset2.view")));

    when(loadablePermissionRepository.findAllByCapabilityId(cap1Id))
      .thenReturn(Stream.of(loadablePermissionEntity(roleId, loadablePermission(roleId, "oldperm1"))));
    when(capabilityService.findByPermissionName("newcap1.view"))
      .thenReturn(Optional.of(capability(cap1Id, "newcap1.view")));
    when(capabilityService.findByPermissionName("newcapset2.view"))
      .thenReturn(Optional.empty());

    when(loadablePermissionRepository.findAllByCapabilitySetId(capSet1Id)).thenReturn(Stream.of(
      loadablePermissionEntity(roleId,
        loadablePermission(roleId, "oldset1"))));
    when(capabilitySetService.findByPermissionName("newcapset1.view")).thenReturn(
      Optional.of(capabilitySet(capSet1Id, "newcapset1.view")));
    when(capabilitySetService.findByPermissionName("newcap2.view")).thenReturn(Optional.empty());

    var publishedEvents = new ArrayList<>();
    doAnswer(inv -> publishedEvents.add(inv.getArgument(0))).when(applicationEventPublisher)
      .publishEvent(any(DomainEvent.class));

    var mockCapSet = new ExtendedCapabilitySet();
    mockCapSet.setId(capSet1Id);
    mockCapSet.setName("capset1.view");
    when(capabilitySetMapper.toExtendedCapabilitySet(any(), any())).thenReturn(mockCapSet);

    var role1Id = randomUUID();
    var role2Id = randomUUID();
    var user1Id = randomUUID();
    var user2Id = randomUUID();

    var oldPermissionToNewPermission =
      Map.of("oldcap1.view", Set.of("newcap1.view", "newcapset2.view"),
        "oldcapset2.view", Set.of("newcapset1.view", "newcap2.view"));
    var oldCapabilityRoleAssignments = Map.of("oldcap1.view", Set.of(role1Id, role2Id));
    var oldCapabilityUserAssignments = Map.of("oldcap1.view", Set.of(user1Id, user2Id));
    var oldCapabilitySetRoleAssignments = Map.of("oldcapset2.view", Set.of(role1Id, role2Id));
    var oldCapabilitySetUserAssignments = Map.of("oldcapset2.view", Set.of(user1Id, user2Id));

    var capabilityReplacements =
      new CapabilityReplacements(oldPermissionToNewPermission, oldCapabilityRoleAssignments,
        oldCapabilityUserAssignments, oldCapabilitySetRoleAssignments, oldCapabilitySetUserAssignments);

    var newLoadablePermissions = new ArrayList<LoadablePermissionEntity>();
    when(loadablePermissionRepository.save(any())).then(inv -> {
      var loadablePermissionEntity = inv.getArgument(0, LoadablePermissionEntity.class);
      newLoadablePermissions.add(loadablePermissionEntity);
      return loadablePermissionEntity;
    });
    var deletedIds = new ArrayList<LoadablePermissionKey>();
    doAnswer(inv -> {
      deletedIds.addAll(inv.getArgument(0));
      return null;
    }).when(loadablePermissionRepository).deleteAllById(any());

    unit.processReplacements(capabilityReplacements);

    verify(roleCapabilityService).create(role1Id, List.of(cap1Id), true);
    verify(roleCapabilityService).create(role2Id, List.of(cap2Id), true);
    verify(roleCapabilitySetService).create(role1Id, List.of(capSet1Id), true);
    verify(roleCapabilitySetService).create(role2Id, List.of(capSet2Id), true);

    verify(userCapabilityService).create(user1Id, List.of(cap1Id));
    verify(userCapabilityService).create(user2Id, List.of(cap2Id));
    verify(userCapabilitySetService).create(user1Id, List.of(capSet1Id));
    verify(userCapabilitySetService).create(user2Id, List.of(capSet2Id));

    assertThat(publishedEvents).hasSize(2);
    var capEvent = (org.folio.roles.domain.model.event.CapabilityEvent) publishedEvents.stream()
      .filter(e -> e instanceof org.folio.roles.domain.model.event.CapabilityEvent).findAny().get();
    var capSetEvent =
      (CapabilitySetEvent) publishedEvents.stream().filter(e -> e instanceof CapabilitySetEvent).findAny().get();
    assertThat(capEvent.getOldObject().getName()).isEqualTo("oldcap1.view");
    assertThat(capEvent.getOldObject().getId()).isEqualTo(cap1Id);
    assertThat(capEvent.getType()).isEqualTo(DELETE);
    assertThat(capSetEvent.getOldObject().getName()).isEqualTo("capset1.view");
    assertThat(capSetEvent.getOldObject().getId()).isEqualTo(capSet1Id);
    assertThat(capSetEvent.getType()).isEqualTo(DELETE);

    assertThat(newLoadablePermissions).hasSize(2);
    newLoadablePermissions.forEach(
      loadablePermissionEntity -> assertThat(loadablePermissionEntity.getRoleId()).isEqualTo(roleId));
    assertThat(newLoadablePermissions.stream()
      .filter(lp -> cap1Id.equals(lp.getCapabilityId()) && lp.getPermissionName().equals("newcap1.view"))).hasSize(1);
    assertThat(newLoadablePermissions.stream().filter(
      lp -> capSet1Id.equals(lp.getCapabilitySetId()) && lp.getPermissionName().equals("newcapset1.view"))).hasSize(1);

    assertThat(deletedIds).hasSize(2);
    assertThat(deletedIds.stream()
      .filter(id -> roleId.equals(id.getRoleId()) && id.getPermissionName().equals("oldperm1"))).hasSize(1);
    assertThat(deletedIds.stream().filter(
      id -> roleId.equals(id.getRoleId()) && id.getPermissionName().equals("oldset1"))).hasSize(1);
  }

  @Test
  void deduceReplacements_positive_emptyIfNothingToDeduce() {
    var testData = new CapabilityEvent();

    var replacements = unit.deduceReplacements(testData);
    assertThat(replacements).isEmpty();
  }

  @Test
  void replaceLoadable_positive_emptyPermissionMappingData() {
    var capabilityReplacements = new CapabilityReplacements(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    unit.replaceLoadable(capabilityReplacements);

    verifyNoInteractions(loadablePermissionRepository);
  }

  private static Capability capability(UUID id, String name) {
    var result = new Capability();
    result.setId(id);
    result.setName(name);
    result.setPermission(name);
    return result;
  }

  private static CapabilitySet capabilitySet(UUID id, String name) {
    var result = new CapabilitySet();
    result.setId(id);
    result.setName(name);
    result.setPermission(name);
    return result;
  }
}
