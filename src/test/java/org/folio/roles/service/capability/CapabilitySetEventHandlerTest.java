package org.folio.roles.service.capability;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.CapabilitySetUtils.CAPABILITY_SET_ID;
import static org.folio.roles.support.CapabilitySetUtils.extendedCapabilitySet;
import static org.folio.roles.support.CapabilityUtils.FOO_CREATE_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_EDIT_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.FOO_VIEW_CAPABILITY;
import static org.folio.roles.support.CapabilityUtils.fooItemCapability;
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
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.UserPolicy;
import org.folio.roles.domain.model.ExtendedCapabilitySet;
import org.folio.roles.domain.model.event.CapabilitySetEvent;
import org.folio.roles.service.permission.RolePermissionService;
import org.folio.roles.service.permission.UserPermissionService;
import org.folio.roles.service.policy.PolicyService;
import org.folio.roles.support.CapabilitySetUtils;
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
class CapabilitySetEventHandlerTest {

  private final FolioExecutionContext context = new DefaultFolioExecutionContext(
    new TestModRolesKeycloakModuleMetadata(), Map.of(OkapiHeaders.TENANT, List.of(TENANT_ID)));
  private final List<UUID> capabilitySetIds = List.of(CAPABILITY_SET_ID);
  private final List<Endpoint> endpoints = List.of(fooItemGetEndpoint());

  @InjectMocks private CapabilitySetEventHandler capabilitySetEventHandler;

  @Mock private PolicyService policyService;
  @Mock private CapabilityService capabilityService;
  @Mock private CapabilitySetService capabilitySetService;
  @Mock private RolePermissionService rolePermissionService;
  @Mock private UserPermissionService userPermissionService;
  @Mock private RoleCapabilitySetService roleCapabilitySetService;
  @Mock private UserCapabilitySetService userCapabilitySetService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_positive_rolePolicy() {
    when(policyService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(emptyList());
    when(capabilityService.findByIds(List.of(FOO_CREATE_CAPABILITY))).thenReturn(List.of(fooCreateCapability()));
    when(rolePermissionService.getAssignedEndpoints(ROLE_ID, emptyList(), capabilitySetIds)).thenReturn(endpoints);

    var newSet = capabilitySet(fooViewCapability(), fooCreateCapability());
    var oldSet = capabilitySet(fooViewCapability(), fooEditCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newSet, oldSet).withContext(context);

    capabilitySetEventHandler.handleCapabilitySetUpdatedEvent(event);

    verify(rolePermissionService).createPermissions(ROLE_ID, List.of(fooItemPostEndpoint()));
    verify(rolePermissionService).deletePermissions(ROLE_ID, List.of(fooItemPutEndpoint()));
    verifyNoInteractions(userPermissionService);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_positive_newCapabilityInSet() {
    when(policyService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(emptyList());
    when(capabilityService.findByIds(List.of(FOO_CREATE_CAPABILITY))).thenReturn(List.of(fooCreateCapability()));

    var newSet = capabilitySet(fooViewCapability(), fooCreateCapability());
    var oldSet = capabilitySet(fooViewCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newSet, oldSet).withContext(context);

    capabilitySetEventHandler.handleCapabilitySetUpdatedEvent(event);

    verify(rolePermissionService).createPermissions(ROLE_ID, List.of(fooItemPostEndpoint()));
    verifyNoInteractions(userPermissionService);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_positive_deprecatedCapabilityInSet() {
    when(policyService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(emptyList());
    when(rolePermissionService.getAssignedEndpoints(ROLE_ID, emptyList(), capabilitySetIds)).thenReturn(endpoints);

    var newSet = capabilitySet(fooViewCapability());
    var oldSet = capabilitySet(fooViewCapability(), fooEditCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newSet, oldSet).withContext(context);

    capabilitySetEventHandler.handleCapabilitySetUpdatedEvent(event);

    verify(rolePermissionService).deletePermissions(ROLE_ID, List.of(fooItemPutEndpoint()));
    verifyNoInteractions(userPermissionService);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_positive_unchangedCapabilitySet() {
    var newSet = capabilitySet(fooViewCapability(), fooEditCapability());
    var oldSet = capabilitySet(fooViewCapability(), fooEditCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newSet, oldSet).withContext(context);

    capabilitySetEventHandler.handleCapabilitySetUpdatedEvent(event);

    verifyNoInteractions(userPermissionService, rolePermissionService);
  }

  @Test
  void handleCapabilitySetUpdatedEvent_positive_userPolicy() {
    when(policyService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(emptyList());
    when(policyService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(userPolicy()));
    when(capabilityService.findByIds(List.of(FOO_CREATE_CAPABILITY))).thenReturn(List.of(fooCreateCapability()));
    when(userPermissionService.getAssignedEndpoints(USER_ID, emptyList(), capabilitySetIds)).thenReturn(endpoints);

    var newSet = capabilitySet(fooViewCapability(), fooCreateCapability());
    var oldSet = capabilitySet(fooViewCapability(), fooEditCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newSet, oldSet).withContext(context);

    capabilitySetEventHandler.handleCapabilitySetUpdatedEvent(event);

    verify(userPermissionService).createPermissions(USER_ID, List.of(fooItemPostEndpoint()));
    verify(userPermissionService).deletePermissions(USER_ID, List.of(fooItemPutEndpoint()));
    verifyNoInteractions(rolePermissionService);
  }

  @ParameterizedTest(name = "[{index}] name={0}")
  @MethodSource("invalidRolePolicyDataSource")
  @DisplayName("handleCapabilityUpdatedEvent_positive_parameterizedForInvalidRolePolicies")
  void handleCapabilitySetUpdatedEvent_positive_parameterizedForInvalidRolePolicies(
    @SuppressWarnings("unused") String name, Policy rolePolicy) {
    when(policyService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(rolePolicy));
    when(policyService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(emptyList());

    var newSet = capabilitySet(fooViewCapability(), fooCreateCapability());
    var oldSet = capabilitySet(fooViewCapability(), fooEditCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newSet, oldSet).withContext(context);

    capabilitySetEventHandler.handleCapabilitySetUpdatedEvent(event);

    verifyNoInteractions(userPermissionService, rolePermissionService);
  }

  @ParameterizedTest(name = "[{index}] name={0}")
  @MethodSource("invalidUserPolicyDataSource")
  @DisplayName("handleCapabilityUpdatedEvent_positive_parameterizedForInvalidRolePolicies")
  void handleCapabilitySetUpdatedEvent_positive_parameterizedForInvalidUserPolicies(
    @SuppressWarnings("unused") String name, Policy userPolicy) {
    var newSet = capabilitySet(fooViewCapability(), fooCreateCapability());
    var oldSet = capabilitySet(fooViewCapability(), fooEditCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.updated(newSet, oldSet).withContext(context);

    when(policyService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(emptyList());
    when(policyService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(userPolicy));

    capabilitySetEventHandler.handleCapabilitySetUpdatedEvent(event);

    verifyNoInteractions(userPermissionService, rolePermissionService);
  }

  @Test
  void handleCapabilitySetDeletedEvent_positive() {
    when(policyService.findRolePoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(rolePolicy()));
    when(policyService.findUserPoliciesByCapabilitySetId(CAPABILITY_SET_ID)).thenReturn(List.of(userPolicy()));

    var capabilitySet = capabilitySet(fooViewCapability(), fooCreateCapability());
    var event = (CapabilitySetEvent) CapabilitySetEvent.deleted(capabilitySet).withContext(context);

    capabilitySetEventHandler.handleCapabilitySetDeletedEvent(event);

    verify(roleCapabilitySetService).delete(ROLE_ID, CAPABILITY_SET_ID);
    verify(userCapabilitySetService).delete(USER_ID, CAPABILITY_SET_ID);
    verify(capabilitySetService).deleteById(CAPABILITY_SET_ID);
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

  private static Capability fooViewCapability() {
    return fooItemCapability(FOO_VIEW_CAPABILITY, CapabilityAction.VIEW, "foo.item.get", fooItemGetEndpoint());
  }

  private static Capability fooCreateCapability() {
    return fooItemCapability(FOO_CREATE_CAPABILITY, CapabilityAction.CREATE, "foo.item.post", fooItemPostEndpoint());
  }

  private static Capability fooEditCapability() {
    return fooItemCapability(FOO_EDIT_CAPABILITY, CapabilityAction.EDIT, "foo.item.put", fooItemPutEndpoint());
  }

  private static ExtendedCapabilitySet capabilitySet(Capability... capabilities) {
    var capabilityIds = mapItems(asList(capabilities), Capability::getId);
    return extendedCapabilitySet(CapabilitySetUtils.capabilitySet(CAPABILITY_SET_ID, capabilityIds), capabilities);
  }
}
