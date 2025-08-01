package org.folio.roles.service.migration;

import static java.util.Collections.emptyList;
import static org.folio.roles.domain.dto.CapabilityAction.MANAGE;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.FOO_MANAGE_CAPABILITIES;
import static org.folio.roles.support.CapabilitySetUtils.capabilitySet;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_MANAGE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.fooItemCapability;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.service.capability.CapabilityService;
import org.folio.roles.service.capability.CapabilitySetService;
import org.folio.roles.service.capability.RoleCapabilityService;
import org.folio.roles.service.capability.RoleCapabilitySetService;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RolePermissionAssignorTest {

  @InjectMocks private RolePermissionAssignor rolePermissionAssignor;

  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private RoleCapabilityService roleCapabilityService;
  @Mock private RoleCapabilitySetService roleCapabilitySetService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void assignPermissions_positive() {
    var idCapabilityToViewCapabilities = UUID.randomUUID();
    var manageCapabilities = List.of(fooItemCapability(idCapabilityToViewCapabilities, VIEW, "collection.get"));
    var permissions = List.of("foo.item.get", "foo.item.post", "foo.item.all");
    var userPermissions = new UserPermissions().userId(USER_ID).role(role())
      .permissions(permissions).manageCapabilities(manageCapabilities);

    when(capabilityService.findByPermissionNames(permissions)).thenReturn(List.of(
      fooItemCapability(FOO_VIEW_CAPABILITY, VIEW, "foo.item.get", fooItemGetEndpoint()),
      fooItemCapability(FOO_CREATE_CAPABILITY, VIEW, "foo.item.post", fooItemPostEndpoint()),
      fooItemCapability(FOO_MANAGE_CAPABILITY, MANAGE, "foo.item.all")));

    when(capabilitySetService.findByPermissionNames(permissions)).thenReturn(List.of(
      capabilitySet(CAPABILITY_SET_ID, "Foo Item", MANAGE, FOO_MANAGE_CAPABILITIES).permission("foo.item.all")));

    rolePermissionAssignor.assignPermissions(List.of(userPermissions, userPermissions));

    var expectedCapabilitiesIds =
      List.of(FOO_VIEW_CAPABILITY, FOO_CREATE_CAPABILITY, FOO_MANAGE_CAPABILITY, idCapabilityToViewCapabilities);
    verify(roleCapabilityService).create(ROLE_ID, expectedCapabilitiesIds, true);
    verify(roleCapabilitySetService).create(ROLE_ID, List.of(CAPABILITY_SET_ID), true);
  }

  @Test
  void assignPermissions_positive_capabilitiesAndCapabilitySetsNotFound() {
    var permissions = List.of("foo.item.get", "foo.item.post", "foo.item.all");
    var userPermissions = new UserPermissions().userId(USER_ID).role(role()).permissions(permissions);
    when(capabilityService.findByPermissionNames(permissions)).thenReturn(emptyList());
    when(capabilitySetService.findByPermissionNames(permissions)).thenReturn(emptyList());

    rolePermissionAssignor.assignPermissions(List.of(userPermissions));

    verify(roleCapabilityService, never()).create(any(), anyList(), anyBoolean());
    verify(roleCapabilitySetService, never()).create(any(), anyList(), anyBoolean());
  }
}
