package org.folio.roles.service.migration;

import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityExistsException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.RoleCapabilityEntity;
import org.folio.roles.domain.entity.RoleCapabilitySetEntity;
import org.folio.roles.domain.entity.UserCapabilityEntity;
import org.folio.roles.domain.entity.UserCapabilitySetEntity;
import org.folio.roles.repository.LoadablePermissionRepository;
import org.folio.roles.repository.RoleCapabilityRepository;
import org.folio.roles.repository.RoleCapabilitySetRepository;
import org.folio.roles.repository.UserCapabilityRepository;
import org.folio.roles.repository.UserCapabilitySetRepository;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.service.capability.UserCapabilityService;
import org.folio.roles.service.capability.UserCapabilitySetService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityDuplicateMigrationServiceTest {

  private static final String OLD_CAPABILITY_NAME = "foo_test.view";
  private static final String NEW_CAPABILITY_NAME = "foo_test.execute";
  private static final UUID OLD_CAP_ID = fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID NEW_CAP_ID = fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OLD_CAP_SET_ID = fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID NEW_CAP_SET_ID = fromString("44444444-4444-4444-4444-444444444444");

  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private RoleCapabilityService roleCapabilityService;
  @Mock private UserCapabilityService userCapabilityService;
  @Mock private RoleCapabilitySetService roleCapabilitySetService;
  @Mock private UserCapabilitySetService userCapabilitySetService;
  @Mock private RoleCapabilityRepository roleCapabilityRepository;
  @Mock private UserCapabilityRepository userCapabilityRepository;
  @Mock private RoleCapabilitySetRepository roleCapabilitySetRepository;
  @Mock private UserCapabilitySetRepository userCapabilitySetRepository;
  @Mock private LoadablePermissionRepository loadablePermissionRepository;

  @InjectMocks private CapabilityDuplicateMigrationService service;

  @Test
  void migrate_positive_capabilityOnly() {
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(capabilityService).deleteById(OLD_CAP_ID);
    verify(capabilitySetService).deleteAllLinksToCapability(OLD_CAP_ID);
    verify(capabilitySetService, never()).deleteById(any(UUID.class));
  }

  @Test
  void migrate_positive_capabilitySetOnly() {
    var oldCapabilitySet = capabilitySet(OLD_CAP_SET_ID);
    var newCapabilitySet = capabilitySet(NEW_CAP_SET_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(userCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(capabilitySetService).deleteById(OLD_CAP_SET_ID);
    verify(capabilityService, never()).deleteById(any(UUID.class));
  }

  @Test
  void migrate_positive_bothCapabilityAndCapabilitySet() {
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    var oldCapabilitySet = capabilitySet(OLD_CAP_SET_ID);
    var newCapabilitySet = capabilitySet(NEW_CAP_SET_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(Stream.empty());
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(userCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(capabilityService).deleteById(OLD_CAP_ID);
    verify(capabilitySetService).deleteById(OLD_CAP_SET_ID);
  }

  @Test
  void migrate_positive_idempotentWhenNothingFound() {
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(roleCapabilityRepository, never()).findAllByCapabilityId(any(UUID.class));
    verify(capabilityService, never()).deleteById(any(UUID.class));
    verify(capabilitySetService, never()).deleteById(any(UUID.class));
  }

  @Test
  void migrate_negative_capabilityExistsButNewNotFound() {
    var oldCapability = capability(OLD_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(roleCapabilityRepository, never()).findAllByCapabilityId(any(UUID.class));
    verify(capabilityService, never()).deleteById(any(UUID.class));
  }

  @Test
  void migrate_negative_capabilitySetExistsButNewNotFound() {
    var oldCapabilitySet = capabilitySet(OLD_CAP_SET_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(roleCapabilitySetRepository, never()).findAllByCapabilitySetId(any(UUID.class));
    verify(capabilitySetService, never()).deleteById(any(UUID.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "  "})
  void migrate_negative_blankOldCapabilityName(String blankName) {
    assertThatThrownBy(() -> service.migrate(blankName, NEW_CAPABILITY_NAME))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Capability names must not be blank");

    verify(capabilityService, never()).findByName(any(String.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "  "})
  void migrate_negative_blankNewCapabilityName(String blankName) {
    assertThatThrownBy(() -> service.migrate(OLD_CAPABILITY_NAME, blankName))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Capability names must not be blank");

    verify(capabilityService, never()).findByName(any(String.class));
  }

  @Test
  void migrate_positive_multipleRoleAssignments() {
    var roleId1 = randomUUID();
    var roleId2 = randomUUID();
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID))
      .thenReturn(List.of(roleCapabilityEntity(roleId1), roleCapabilityEntity(roleId2)));
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    var inOrder = inOrder(roleCapabilityService);
    inOrder.verify(roleCapabilityService).create(eq(roleId1), eq(List.of(NEW_CAP_ID)), anyBoolean());
    inOrder.verify(roleCapabilityService).delete(eq(roleId1), eq(OLD_CAP_ID));
    inOrder.verify(roleCapabilityService).create(eq(roleId2), eq(List.of(NEW_CAP_ID)), anyBoolean());
    inOrder.verify(roleCapabilityService).delete(eq(roleId2), eq(OLD_CAP_ID));
  }

  @Test
  void migrate_positive_multipleUserCapabilities() {
    var userId1 = randomUUID();
    var userId2 = randomUUID();
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID))
      .thenReturn(List.of(userCapabilityEntity(userId1), userCapabilityEntity(userId2)));
    when(userCapabilityRepository.existsByUserIdAndCapabilityId(userId1, NEW_CAP_ID))
      .thenReturn(false);
    when(userCapabilityRepository.existsByUserIdAndCapabilityId(userId2, NEW_CAP_ID))
      .thenReturn(false);
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(userCapabilityService, times(2)).create(any(UUID.class), argThat(list -> list.contains(NEW_CAP_ID)));
    verify(userCapabilityService).delete(userId1, OLD_CAP_ID);
    verify(userCapabilityService).delete(userId2, OLD_CAP_ID);
  }

  @Test
  void migrate_positive_userCapabilityAlreadyExists() {
    var userId = randomUUID();
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID))
      .thenReturn(List.of(userCapabilityEntity(userId)));
    when(userCapabilityRepository.existsByUserIdAndCapabilityId(userId, NEW_CAP_ID))
      .thenReturn(true);
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(userCapabilityService, never()).create(any(UUID.class), any());
    verify(userCapabilityService).delete(userId, OLD_CAP_ID);
  }

  @Test
  void migrate_positive_userCapabilityEntityExistsException() {
    var userId = randomUUID();
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID))
      .thenReturn(List.of(userCapabilityEntity(userId)));
    when(userCapabilityRepository.existsByUserIdAndCapabilityId(userId, NEW_CAP_ID))
      .thenReturn(false);
    when(userCapabilityService.create(userId, List.of(NEW_CAP_ID)))
      .thenThrow(EntityExistsException.class);
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(userCapabilityService).delete(userId, OLD_CAP_ID);
  }

  @Test
  void migrate_positive_multipleRoleCapabilitySets() {
    var roleId1 = randomUUID();
    var roleId2 = randomUUID();
    var oldCapabilitySet = capabilitySet(OLD_CAP_SET_ID);
    var newCapabilitySet = capabilitySet(NEW_CAP_SET_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID))
      .thenReturn(List.of(roleCapabilitySetEntity(roleId1), roleCapabilitySetEntity(roleId2)));
    when(userCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    var inOrder = inOrder(roleCapabilitySetService);
    inOrder.verify(roleCapabilitySetService).create(eq(roleId1), eq(List.of(NEW_CAP_SET_ID)), anyBoolean());
    inOrder.verify(roleCapabilitySetService).delete(eq(roleId1), eq(OLD_CAP_SET_ID));
    inOrder.verify(roleCapabilitySetService).create(eq(roleId2), eq(List.of(NEW_CAP_SET_ID)), anyBoolean());
    inOrder.verify(roleCapabilitySetService).delete(eq(roleId2), eq(OLD_CAP_SET_ID));
  }

  @Test
  void migrate_positive_userCapabilitySetAlreadyExists() {
    var userId = randomUUID();
    var oldCapabilitySet = capabilitySet(OLD_CAP_SET_ID);
    var newCapabilitySet = capabilitySet(NEW_CAP_SET_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(userCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID))
      .thenReturn(List.of(userCapabilitySetEntity(userId)));
    when(userCapabilitySetRepository.existsByUserIdAndCapabilitySetId(userId, NEW_CAP_SET_ID))
      .thenReturn(true);
    when(loadablePermissionRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(userCapabilitySetService, never()).create(any(UUID.class), any());
    verify(userCapabilitySetService).delete(userId, OLD_CAP_SET_ID);
  }

  @Test
  void migrate_positive_userCapabilitySetEntityExistsException() {
    var userId = randomUUID();
    var oldCapabilitySet = capabilitySet(OLD_CAP_SET_ID);
    var newCapabilitySet = capabilitySet(NEW_CAP_SET_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(userCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID))
      .thenReturn(List.of(userCapabilitySetEntity(userId)));
    when(userCapabilitySetRepository.existsByUserIdAndCapabilitySetId(userId, NEW_CAP_SET_ID))
      .thenReturn(false);
    when(userCapabilitySetService.create(userId, List.of(NEW_CAP_SET_ID)))
      .thenThrow(EntityExistsException.class);
    when(loadablePermissionRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(userCapabilitySetService).delete(userId, OLD_CAP_SET_ID);
  }

  @Test
  void migrate_positive_loadablePermissionsCapabilityUpdated() {
    var permission1 = loadablePermissionEntity(randomUUID(), OLD_CAP_ID, null);
    var permission2 = loadablePermissionEntity(randomUUID(), OLD_CAP_ID, null);
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID))
      .thenReturn(Stream.of(permission1, permission2));

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(loadablePermissionRepository).saveAll(argThat(permissions ->
      permissions != null && ((List<LoadablePermissionEntity>) permissions).stream()
        .allMatch(p -> NEW_CAP_ID.equals(p.getCapabilityId()))
    ));
  }

  @Test
  void migrate_positive_loadablePermissionsCapabilitySetUpdated() {
    var permission1 = loadablePermissionEntity(randomUUID(), null, OLD_CAP_SET_ID);
    var permission2 = loadablePermissionEntity(randomUUID(), null, OLD_CAP_SET_ID);
    var oldCapabilitySet = capabilitySet(OLD_CAP_SET_ID);
    var newCapabilitySet = capabilitySet(NEW_CAP_SET_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapabilitySet));
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapabilitySet));
    when(roleCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(userCapabilitySetRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilitySetId(OLD_CAP_SET_ID))
      .thenReturn(Stream.of(permission1, permission2));

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(loadablePermissionRepository).saveAll(argThat(permissions ->
      permissions != null && ((List<LoadablePermissionEntity>) permissions).stream()
        .allMatch(p -> NEW_CAP_SET_ID.equals(p.getCapabilitySetId()))
    ));
  }

  @Test
  void positive_loadablePermissionsNoResults() {
    var oldCapability = capability(OLD_CAP_ID);
    var newCapability = capability(NEW_CAP_ID);
    when(capabilityService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.of(oldCapability));
    when(capabilityService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.of(newCapability));
    when(capabilitySetService.findByName(OLD_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(capabilitySetService.findByName(NEW_CAPABILITY_NAME)).thenReturn(Optional.empty());
    when(roleCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(userCapabilityRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(emptyList());
    when(loadablePermissionRepository.findAllByCapabilityId(OLD_CAP_ID)).thenReturn(Stream.empty());

    service.migrate(OLD_CAPABILITY_NAME, NEW_CAPABILITY_NAME);

    verify(loadablePermissionRepository, never()).saveAll(any());
  }

  private static RoleCapabilityEntity roleCapabilityEntity(UUID roleId) {
    var entity = new RoleCapabilityEntity();
    entity.setRoleId(roleId);
    entity.setCapabilityId(OLD_CAP_ID);
    return entity;
  }

  private static UserCapabilityEntity userCapabilityEntity(UUID userId) {
    var entity = new UserCapabilityEntity();
    entity.setUserId(userId);
    entity.setCapabilityId(OLD_CAP_ID);
    return entity;
  }

  private static RoleCapabilitySetEntity roleCapabilitySetEntity(UUID roleId) {
    var entity = new RoleCapabilitySetEntity();
    entity.setRoleId(roleId);
    entity.setCapabilitySetId(OLD_CAP_SET_ID);
    return entity;
  }

  private static UserCapabilitySetEntity userCapabilitySetEntity(UUID userId) {
    var entity = new UserCapabilitySetEntity();
    entity.setUserId(userId);
    entity.setCapabilitySetId(OLD_CAP_SET_ID);
    return entity;
  }

  private static LoadablePermissionEntity loadablePermissionEntity(UUID roleId, UUID capabilityId,
    UUID capabilitySetId) {
    var entity = new LoadablePermissionEntity();
    entity.setRoleId(roleId);
    entity.setPermissionName("test.permission");
    entity.setCapabilityId(capabilityId);
    entity.setCapabilitySetId(capabilitySetId);
    return entity;
  }
}
