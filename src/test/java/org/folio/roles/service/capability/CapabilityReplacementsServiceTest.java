package org.folio.roles.service.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.permission.model.PermissionAction.VIEW;
import static org.folio.common.utils.permission.model.PermissionType.DATA;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.integration.kafka.model.ModuleType.MODULE;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.integration.kafka.mapper.CapabilitySetMapper;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.folio.roles.integration.kafka.model.FolioResource;
import org.folio.roles.integration.kafka.model.Permission;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.roles.service.permission.PermissionOverrider;
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
  @Mock private PermissionOverrider permissionOverrider;
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

  @Test
  void testDeduceReplacementsPositive() {
    when(permissionOverrider.getPermissionMappings()).thenReturn(Map.of("old-perm2.get",
      PermissionData.builder().permissionName("old-perm-two.get").action(VIEW).resource("resource2").type(DATA)
        .build()));

    var testData = new CapabilityEvent();
    testData.setModuleId("fake-module");
    testData.setApplicationId("fake-application");
    testData.setModuleType(MODULE);
    testData.setResources(List.of(new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint1").method(GET)))
        .permission(new Permission().permissionName("new-perm.get").replaces(List.of("old-perm.get"))),
      new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint2").method(GET))).permission(
        new Permission().permissionName("new-perm2.get").replaces(List.of("old-perm2.get"))
          .subPermissions(List.of("A", "B"))),
      new FolioResource().endpoints(List.of(new Endpoint().path("/endpoint3").method(GET)))
        .permission(new Permission().permissionName("new-perm3.get").replaces(List.of("old-perm.get")))));

    var capabilityUuid = UUID.randomUUID();
    var capabilitySetUuid = UUID.randomUUID();
    when(capabilityService.findByNames(Set.of("old-perm.view", "old-perm-two.view"))).thenReturn(
      List.of(new Capability().id(capabilityUuid).name("old-perm.view")));
    when(capabilitySetService.findByNames(Set.of("old-perm.view", "old-perm-two.view"))).thenReturn(
      List.of(new CapabilitySet().id(capabilitySetUuid).name("old-perm-two.view")));

    var role1Uuid = UUID.randomUUID();
    var role2Uuid = UUID.randomUUID();
    var role3Uuid = UUID.randomUUID();
    var userUuid = UUID.randomUUID();
    var user2Uuid = UUID.randomUUID();
    var user3Uuid = UUID.randomUUID();
    when(roleCapabilityRepository.findAllByCapabilityId(capabilityUuid)).thenReturn(
      List.of(RoleCapabilityEntity.of(role1Uuid, null)));
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(capabilitySetUuid)).thenReturn(
      List.of(RoleCapabilitySetEntity.of(role2Uuid, null), RoleCapabilitySetEntity.of(role3Uuid, null)));

    when(userCapabilityRepository.findAllByCapabilityId(capabilityUuid)).thenReturn(
      List.of(UserCapabilityEntity.of(userUuid, null)));
    when(userCapabilitySetRepository.findAllByCapabilitySetId(capabilitySetUuid)).thenReturn(
      List.of(UserCapabilitySetEntity.of(user2Uuid, null), UserCapabilitySetEntity.of(user3Uuid, null)));

    var replacements = unit.deduceReplacements(testData);
    assertThat(replacements.isPresent()).isTrue();
    assertThat(replacements.get().oldCapabilitiesToNewCapabilities()).isEqualTo(
      Map.of("old-perm.view", Set.of("new-perm3.view", "new-perm.view"), "old-perm-two.view",
        Set.of("new-perm2.view")));

    assertThat(replacements.get().oldCapabilityRoleAssignments()).isEqualTo(Map.of("old-perm.view", Set.of(role1Uuid)));
    assertThat(replacements.get().oldCapabilitySetRoleAssignments()).isEqualTo(
      Map.of("old-perm-two.view", Set.of(role2Uuid, role3Uuid)));
    assertThat(replacements.get().oldCapabilityUserAssignments()).isEqualTo(Map.of("old-perm.view", Set.of(userUuid)));
    assertThat(replacements.get().oldCapabilitySetUserAssignments()).isEqualTo(
      Map.of("old-perm-two.view", Set.of(user2Uuid, user3Uuid)));
  }

  @Test
  void testDeduceReplacementsNoReplacements() {
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
    assertThat(replacements.isPresent()).isFalse();
  }
}
