package org.folio.roles.service.capability;

import static java.util.Collections.emptyList;
import static org.folio.roles.support.CapabilityUtils.CAPABILITY_ID;
import static org.folio.roles.support.CapabilityUtils.capability;
import static org.folio.roles.support.EndpointUtils.fooItemGetEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPostEndpoint;
import static org.folio.roles.support.EndpointUtils.fooItemPutEndpoint;
import static org.folio.roles.support.PolicyUtils.rolePolicy;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.folio.common.utils.OkapiHeaders;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.UserPolicy;
import org.folio.roles.domain.model.event.CapabilityEvent;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.permission.UserPermissionService;
import org.folio.roles.service.policy.PolicyService;
import org.folio.roles.support.TestUtils;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilityEventHandlerTest {

  private final FolioExecutionContext context = new DefaultFolioExecutionContext(
    new TestModRolesKeycloakModuleMetadata(), Map.of(OkapiHeaders.TENANT, List.of(TENANT_ID)));

  private final List<UUID> capabilityIds = List.of(CAPABILITY_ID);
  private final List<Endpoint> endpoints = List.of(fooItemGetEndpoint());

  @InjectMocks private CapabilityEventHandler capabilityEventHandler;
  @Mock private PolicyService policyService;
  @Mock private CapabilityService capabilityService;

  @Mock private CapabilitySetService capabilitySetService;
  @Mock private RolePermissionService rolePermissionService;
  @Mock private RoleCapabilityService roleCapabilityService;
  @Mock private UserPermissionService userPermissionService;
  @Mock private UserCapabilityService userCapabilityService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void handleCapabilityUpdatedEvent_positive_rolePolicy() {
    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());
    when(rolePermissionService.getAssignedEndpoints(ROLE_ID, capabilityIds, emptyList())).thenReturn(endpoints);

    var newCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPutEndpoint());
    var oldCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.updated(newCapability, oldCapability).withContext(context);

    capabilityEventHandler.handleCapabilityUpdatedEvent(event);

    verify(rolePermissionService).createPermissions(ROLE_ID, List.of(fooItemPutEndpoint()));
    verify(rolePermissionService).deletePermissions(ROLE_ID, List.of(fooItemPostEndpoint()));
    verifyNoInteractions(userPermissionService);
  }

  @Test
  void handleCapabilityUpdatedEvent_positive_newEndpoints() {
    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());
    when(rolePermissionService.getAssignedEndpoints(ROLE_ID, capabilityIds, emptyList())).thenReturn(endpoints);

    var newCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var oldCapability = capability(CAPABILITY_ID, fooItemGetEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.updated(newCapability, oldCapability).withContext(context);

    capabilityEventHandler.handleCapabilityUpdatedEvent(event);

    verify(rolePermissionService).createPermissions(ROLE_ID, List.of(fooItemPostEndpoint()));
    verifyNoInteractions(userPermissionService);
  }

  @Test
  void handleCapabilityUpdatedEvent_positive_deprecatedEndpoints() {
    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());
    when(rolePermissionService.getAssignedEndpoints(ROLE_ID, capabilityIds, emptyList())).thenReturn(endpoints);

    var newCapability = capability(CAPABILITY_ID, fooItemGetEndpoint());
    var oldCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.updated(newCapability, oldCapability).withContext(context);

    capabilityEventHandler.handleCapabilityUpdatedEvent(event);

    verify(rolePermissionService).deletePermissions(ROLE_ID, List.of(fooItemPostEndpoint()));
    verifyNoInteractions(userPermissionService);
  }

  @Test
  void handleCapabilityUpdatedEvent_positive_sameEndpoints() {
    var newCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var oldCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.updated(newCapability, oldCapability).withContext(context);

    capabilityEventHandler.handleCapabilityUpdatedEvent(event);

    verifyNoInteractions(policyService, userPermissionService, rolePermissionService);
  }

  @Test
  void handleCapabilityUpdatedEvent_positive_userPolicy() {
    var existingEndpoints = List.of(fooItemGetEndpoint());
    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(userPolicy()));
    when(userPermissionService.getAssignedEndpoints(USER_ID, capabilityIds, emptyList())).thenReturn(existingEndpoints);

    var newCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPutEndpoint());
    var oldCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.updated(newCapability, oldCapability).withContext(context);

    capabilityEventHandler.handleCapabilityUpdatedEvent(event);

    verify(userPermissionService).createPermissions(USER_ID, List.of(fooItemPutEndpoint()));
    verify(userPermissionService).deletePermissions(USER_ID, List.of(fooItemPostEndpoint()));
    verifyNoInteractions(roleCapabilityService);
  }

  @ParameterizedTest(name = "[{index}] name={0}")
  @MethodSource("invalidRolePolicyDataSource")
  @DisplayName("handleCapabilityUpdatedEvent_positive_parameterizedForInvalidRolePolicies")
  void handleCapabilityUpdatedEvent_positive_parameterizedForInvalidRolePolicies(
    @SuppressWarnings("unused") String name, Policy rolePolicy) {
    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(rolePolicy));
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());

    var newCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPutEndpoint());
    var oldCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.updated(newCapability, oldCapability).withContext(context);

    capabilityEventHandler.handleCapabilityUpdatedEvent(event);

    verifyNoInteractions(userPermissionService, rolePermissionService);
  }

  @ParameterizedTest(name = "[{index}] name={0}")
  @MethodSource("invalidUserPolicyDataSource")
  @DisplayName("handleCapabilityUpdatedEvent_positive_parameterizedForInvalidRolePolicies")
  void handleCapabilityUpdatedEvent_positive_parameterizedForInvalidUserPolicies(
    @SuppressWarnings("unused") String name, Policy userPolicy) {
    var newCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPutEndpoint());
    var oldCapability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.updated(newCapability, oldCapability).withContext(context);

    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(userPolicy));

    capabilityEventHandler.handleCapabilityUpdatedEvent(event);

    verifyNoInteractions(userPermissionService, rolePermissionService);
  }

  @Test
  void handleCapabilityDeletedEvent_positive_rolePolicy() {
    var existingEndpoints = List.of(fooItemGetEndpoint());
    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());
    when(rolePermissionService.getAssignedEndpoints(ROLE_ID, capabilityIds, emptyList())).thenReturn(existingEndpoints);

    var capability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.deleted(capability).withContext(context);

    capabilityEventHandler.handleCapabilityDeletedEvent(event);

    verify(rolePermissionService).deletePermissions(ROLE_ID, List.of(fooItemPostEndpoint()));
    verify(roleCapabilityService).delete(ROLE_ID, CAPABILITY_ID);
    verify(capabilityService).deleteById(CAPABILITY_ID);
    verify(capabilitySetService).deleteAllLinksToCapability(CAPABILITY_ID);
    verifyNoInteractions(userPermissionService, userCapabilityService);
  }

  @Test
  void handleCapabilityDeletedEvent_positive_userPolicy() {
    var existingEndpoints = List.of(fooItemGetEndpoint());
    when(policyService.findRolePoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(emptyList());
    when(policyService.findUserPoliciesByCapabilityId(CAPABILITY_ID)).thenReturn(List.of(userPolicy()));
    when(userPermissionService.getAssignedEndpoints(USER_ID, capabilityIds, emptyList())).thenReturn(existingEndpoints);

    var capability = capability(CAPABILITY_ID, fooItemGetEndpoint(), fooItemPostEndpoint());
    var event = (CapabilityEvent) CapabilityEvent.deleted(capability).withContext(context);

    capabilityEventHandler.handleCapabilityDeletedEvent(event);

    verify(userPermissionService).deletePermissions(USER_ID, List.of(fooItemPostEndpoint()));
    verify(userCapabilityService).delete(USER_ID, CAPABILITY_ID);
    verify(capabilityService).deleteById(CAPABILITY_ID);
    verify(capabilitySetService).deleteAllLinksToCapability(CAPABILITY_ID);
    verifyNoInteractions(rolePermissionService, roleCapabilityService);
  }

  private static Stream<Arguments> invalidRolePolicyDataSource() {
    return Stream.of(
      arguments("null role policy", new Policy()),
      arguments("null role policy id holder", new Policy().rolePolicy(new RolePolicy())),
      arguments("null role policy id", new Policy().rolePolicy(new RolePolicy().addRolesItem(new RolePolicyRole())))
    );
  }

  private static Stream<Arguments> invalidUserPolicyDataSource() {
    return Stream.of(
      arguments("null user policy", new Policy()),
      arguments("null user policy id", new Policy().userPolicy(new UserPolicy()))
    );
  }
}
