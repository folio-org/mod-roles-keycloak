package org.folio.roles.service.loadablerole;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissions;
import static org.folio.roles.support.TestUtils.copy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.service.event.CapabilityCollectionEvent;
import org.folio.roles.service.event.CapabilitySetEvent;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.instancio.junit.InstancioExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
@ExtendWith(InstancioExtension.class)
class LoadableRoleCapabilityAssignmentProcessorTest {

  @InjectMocks private LoadableRoleCapabilityAssignmentProcessor processor;
  @Mock private LoadablePermissionService service;
  @Mock private CapabilityService capabilityService;
  @Mock private RoleCapabilityService roleCapabilityService;
  @Mock private RoleCapabilitySetService roleCapabilitySetService;
  private FolioExecutionContext context;

  @BeforeEach
  void setUp() {
    context = new FolioExecutionContext() {
      @Override
      public FolioModuleMetadata getFolioModuleMetadata() {
        return new FolioModuleMetadata() {
          @Override
          public String getModuleName() {
            return null;
          }

          @Override
          public String getDBSchemaName(String tenantId) {
            return null;
          }
        };
      }
    };
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void handleCapabilitiesCreatedEvent_positive() {
    var capability1 = capability(randomUUID(), "permission1");
    var capability2 = capability(randomUUID(), "permission2");
    var capabilities = List.of(capability1, capability2);
    final var event = (CapabilityCollectionEvent<List<Capability>>) CapabilityCollectionEvent.created(capabilities)
      .withContext(context);

    var perms = loadablePermissions(10);
    for (int i = 0; i < perms.size(); i++) {
      var perm = perms.get(i);
      perm.setPermissionName(i % 2 == 0 ? capability1.getPermission() : capability2.getPermission());
      perm.setCapabilityId(null); // reset capset id, it should be populated by the processor
    }

    when(service.findAllByPermissions(new HashSet<>(mapItems(capabilities, Capability::getPermission))))
      .thenReturn(perms);

    perms.forEach(perm -> {
      var capabilityId = capabilityIdByPermName(capabilities, perm.getPermissionName());
      when(roleCapabilityService.create(perm.getRoleId(), List.of(capabilityId))).thenReturn(null);

      var permsWithCapabilityId = List.of(copy(perm).capabilityId(capabilityId));
      when(service.saveAll(permsWithCapabilityId)).thenReturn(permsWithCapabilityId);
    });

    processor.handleCapabilitiesCreatedEvent(event);
  }

  @Test
  void handleCapabilitiesCreatedEvent_positive_loadablePermissionsNotFound() {
    var capability1 = capability(randomUUID(), "permission1");
    var capability2 = capability(randomUUID(), "permission2");
    var capabilities = List.of(capability1, capability2);
    final var event = (CapabilityCollectionEvent<List<Capability>>) CapabilityCollectionEvent.created(capabilities)
      .withContext(context);

    when(service.findAllByPermissions(new HashSet<>(mapItems(capabilities, Capability::getPermission))))
      .thenReturn(emptyList());

    processor.handleCapabilitiesCreatedEvent(event);
  }

  @Test
  void handleCapabilitiesCreatedEvent_positive_emptyCapabilitiesInEvent() {
    var event = (CapabilityCollectionEvent<List<Capability>>) CapabilityCollectionEvent.created(emptyList())
      .withContext(context);

    assertThatThrownBy(() -> processor.handleCapabilitiesCreatedEvent(event))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Capabilities Created event contains no data");
  }

  @Test
  void handleCapabilitySetCreatedEvent_positive() {
    var capabilitySet = capabilitySet();
    final var event = (CapabilitySetEvent) CapabilitySetEvent.created(capabilitySet).withContext(context);
    var capability = capability();

    var perms = loadablePermissions(10);
    perms.forEach(perm -> {
      perm.setPermissionName(capability.getPermission());
      perm.setCapabilitySetId(null); // reset capset id, it should be populated by the processor
    });

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(List.of(capability));
    when(service.findAllByPermissions(List.of(capability.getPermission()))).thenReturn(perms);
    perms.forEach(perm -> {
      when(roleCapabilitySetService.create(perm.getRoleId(), List.of(capabilitySet.getId()))).thenReturn(null);

      var permWithCapabilitySetId = copy(perm).capabilitySetId(capabilitySet.getId());
      when(service.save(permWithCapabilitySetId)).thenReturn(permWithCapabilitySetId);
    });

    processor.handleCapabilitySetCreatedEvent(event);
  }

  @Test
  void handleCapabilitySetCreatedEvent_positive_loadablePermissionsNotFound() {
    var capabilitySet = capabilitySet();
    final var event = (CapabilitySetEvent) CapabilitySetEvent.created(capabilitySet).withContext(context);
    var capability = capability();

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(List.of(capability));
    when(service.findAllByPermissions(List.of(capability.getPermission()))).thenReturn(emptyList());

    processor.handleCapabilitySetCreatedEvent(event);
  }

  @Test
  void handleCapabilitySetCreatedEvent_negative_capabilityNotFound() {
    var capabilitySet = capabilitySet();
    var event = (CapabilitySetEvent) CapabilitySetEvent.created(capabilitySet).withContext(context);

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(emptyList());

    assertThatThrownBy(() -> processor.handleCapabilitySetCreatedEvent(event))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage("Single capability is not found by capability set name: %s", capabilitySet.getName());
  }

  @Test
  void handleCapabilitySetUpdatedEvent_positive() {
    var oldCapabilitySet = capabilitySet();
    var newCapabilitySet = capabilitySet(List.of(randomUUID()));
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newCapabilitySet, oldCapabilitySet)
      .withContext(context);

    processor.handleCapabilitySetUpdatedEvent(event);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_negative_capabilitySetIdMismatch() {
    var oldCapabilitySet = capabilitySet();
    var newCapabilitySet = capabilitySet(randomUUID(), List.of(randomUUID()));
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newCapabilitySet, oldCapabilitySet)
      .withContext(context);

    assertThatThrownBy(() -> processor.handleCapabilitySetUpdatedEvent(event))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Ids of old and new version of capability set don't match");
  }

  @Test
  void handleCapabilitySetDeletedEvent_positive() {
    var capabilitySet = capabilitySet();
    final var event = (CapabilitySetEvent) CapabilitySetEvent.deleted(capabilitySet).withContext(context);
    var capability = capability();

    var perms = loadablePermissions(10);
    perms.forEach(perm -> {
      perm.setPermissionName(capability.getPermission());
      perm.setCapabilitySetId(capabilitySet.getId()); // set capset id, it should be reset by the processor
    });

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(List.of(capability));
    when(service.findAllByPermissions(List.of(capability.getPermission()))).thenReturn(perms);
    perms.forEach(perm -> {
      doNothing().when(roleCapabilitySetService).delete(perm.getRoleId(), capabilitySet.getId());

      var permWithoutCapabilitySetId = copy(perm).capabilitySetId(null);
      when(service.save(permWithoutCapabilitySetId)).thenReturn(permWithoutCapabilitySetId);
    });

    processor.handleCapabilitySetDeletedEvent(event);
  }

  @Test
  void handleCapabilitySetDeletedEvent_positive_loadablePermissionsNotFound() {
    var capabilitySet = capabilitySet();
    final var event = (CapabilitySetEvent) CapabilitySetEvent.deleted(capabilitySet).withContext(context);
    var capability = capability();

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(List.of(capability));
    when(service.findAllByPermissions(List.of(capability.getPermission()))).thenReturn(emptyList());

    processor.handleCapabilitySetDeletedEvent(event);
  }

  @Test
  void handleCapabilitySetDeletedEvent_negative_capabilityNotFound() {
    var capabilitySet = capabilitySet();
    var event = (CapabilitySetEvent) CapabilitySetEvent.deleted(capabilitySet).withContext(context);

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(emptyList());

    assertThatThrownBy(() -> processor.handleCapabilitySetDeletedEvent(event))
      .isInstanceOf(EntityNotFoundException.class)
      .hasMessage("Single capability is not found by capability set name: %s", capabilitySet.getName());
  }

  private static UUID capabilityIdByPermName(List<Capability> capabilities, String permissionName) {
    for (Capability capability : capabilities) {
      if (Objects.equals(capability.getPermission(), permissionName)) {
        return capability.getId();
      }
    }
    throw new IllegalStateException("Invalid capability list: no capability with permission name " + permissionName);
  }
}
