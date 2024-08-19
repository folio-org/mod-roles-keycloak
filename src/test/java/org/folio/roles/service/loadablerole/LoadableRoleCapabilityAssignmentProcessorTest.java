package org.folio.roles.service.loadablerole;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilitySetUtils.extendedCapabilitySet;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissions;
import static org.folio.roles.support.TestUtils.copy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.support.TestUtils;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
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
    context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void handleCapabilitiesCreatedEvent_positive() {
    var permission = "permission1";
    var capabilityId = randomUUID();

    var perms = loadablePermissions(5);
    for (LoadablePermission perm : perms) {
      perm.setPermissionName(permission);
      perm.setCapabilityId(null); // reset capset id, it should be populated by the processor
    }

    when(service.findAllByPermissions(List.of(permission))).thenReturn(perms);

    perms.forEach(perm -> {
      when(roleCapabilityService.create(perm.getRoleId(), List.of(capabilityId), false)).thenReturn(null);

      var permsWithCapabilityId = List.of(copy(perm).capabilityId(capabilityId));
      when(service.saveAll(permsWithCapabilityId)).thenReturn(permsWithCapabilityId);
    });

    var capability = capability(capabilityId, permission);
    var event = (CapabilityEvent) CapabilityEvent.created(capability).withContext(context);
    processor.handleCapabilitiesCreatedEvent(event);

    var firstLoadablePermission = perms.get(0);
    verify(roleCapabilityService).create(firstLoadablePermission.getRoleId(), List.of(capabilityId), false);
    verify(service).saveAll(List.of(copy(firstLoadablePermission).capabilityId(capabilityId)));
  }

  @Test
  void handleCapabilitiesCreatedEvent_positive_loadablePermissionsNotFound() {
    var permission = "permission1";
    var capability = capability(randomUUID(), permission);
    var event = (CapabilityEvent) CapabilityEvent.created(capability).withContext(context);
    when(service.findAllByPermissions(List.of(permission))).thenReturn(emptyList());

    processor.handleCapabilitiesCreatedEvent(event);

    verifyNoInteractions(roleCapabilityService, roleCapabilitySetService);
  }

  @Test
  void handleCapabilitySetCreatedEvent_positive() {
    var capabilitySet = capabilitySet();
    var capability = capability();

    var perms = loadablePermissions(10);
    perms.forEach(perm -> {
      perm.setPermissionName(capability.getPermission());
      perm.setCapabilitySetId(null); // reset capset id, it should be populated by the processor
    });

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(List.of(capability));
    when(service.findAllByPermissions(List.of(capability.getPermission()))).thenReturn(perms);
    perms.forEach(perm -> {
      when(roleCapabilitySetService.create(perm.getRoleId(), List.of(capabilitySet.getId()), false)).thenReturn(null);

      var permWithCapabilitySetId = copy(perm).capabilitySetId(capabilitySet.getId());
      when(service.save(permWithCapabilitySetId)).thenReturn(permWithCapabilitySetId);
    });

    var extendedCapabilitySet = extendedCapabilitySet(capabilitySet, emptyList());
    var event = (CapabilitySetEvent) CapabilitySetEvent.created(extendedCapabilitySet).withContext(context);
    processor.handleCapabilitySetCreatedEvent(event);

    var firstLoadablePermission = perms.get(0);
    verify(roleCapabilitySetService).create(firstLoadablePermission.getRoleId(), List.of(CAPABILITY_SET_ID), false);
    verify(service).save(copy(firstLoadablePermission).capabilitySetId(CAPABILITY_SET_ID));
  }

  @Test
  void handleCapabilitySetCreatedEvent_positive_loadablePermissionsNotFound() {
    var capabilitySet = capabilitySet();
    var extendedCapabilitySet = extendedCapabilitySet(capabilitySet, emptyList());
    var event = (CapabilitySetEvent) CapabilitySetEvent.created(extendedCapabilitySet).withContext(context);
    var capability = capability();

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(List.of(capability));
    when(service.findAllByPermissions(List.of(capability.getPermission()))).thenReturn(emptyList());

    processor.handleCapabilitySetCreatedEvent(event);

    verifyNoInteractions(roleCapabilityService, roleCapabilitySetService);
  }

  @Test
  void handleCapabilitySetCreatedEvent_positive_capabilityNotFound() {
    var capabilitySet = capabilitySet();
    var extendedCapabilitySet = extendedCapabilitySet(capabilitySet, emptyList());
    var event = (CapabilitySetEvent) CapabilitySetEvent.created(extendedCapabilitySet).withContext(context);

    when(capabilityService.findByNames(List.of(capabilitySet.getName()))).thenReturn(emptyList());

    processor.handleCapabilitySetCreatedEvent(event);

    verifyNoInteractions(roleCapabilityService, roleCapabilitySetService, service);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_positive() {
    var oldCapabilitySet = extendedCapabilitySet(capabilitySet(), emptyList());
    var newCapabilitySet = extendedCapabilitySet(capabilitySet(List.of(randomUUID())), emptyList());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newCapabilitySet, oldCapabilitySet)
      .withContext(context);

    processor.handleCapabilitySetUpdatedEvent(event);

    verifyNoInteractions(roleCapabilityService, roleCapabilitySetService, service);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_negative_capabilitySetIdMismatch() {
    var oldCapabilitySet = extendedCapabilitySet(capabilitySet(), emptyList());
    var newCapabilitySet = extendedCapabilitySet(capabilitySet(randomUUID(), List.of(randomUUID())), emptyList());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newCapabilitySet, oldCapabilitySet)
      .withContext(context);

    assertThatThrownBy(() -> processor.handleCapabilitySetUpdatedEvent(event))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Ids of old and new version of capability set don't match");
  }
}
