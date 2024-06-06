package org.folio.roles.service.permission;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.PolicyUtils.rolePolicy;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.role;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationService;
import org.folio.roles.service.policy.PolicyService;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RolePermissionServiceTest {

  @InjectMocks private RolePermissionService rolePermissionService;
  @Mock private RoleService roleService;
  @Mock private PolicyService policyService;
  @Mock private KeycloakAuthorizationService keycloakAuthService;

  @Captor private ArgumentCaptor<Supplier<Policy>> newPolicyCaptor;
  @Captor private ArgumentCaptor<Function<Endpoint, String>> nameGeneratorCaptor;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("createPermissions")
  class CreatePermissions {

    @Test
    void positive() {
      var policyName = "Policy for role: " + ROLE_ID;
      var policy = rolePolicy(policyName);
      var endpoint = endpoint("/foo/entities", GET);
      var endpoints = List.of(endpoint);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(policyService.getOrCreatePolicy(eq(policyName), eq(ROLE), newPolicyCaptor.capture())).thenReturn(policy);
      doNothing().when(keycloakAuthService).createPermissions(eq(policy), eq(endpoints), nameGeneratorCaptor.capture());

      rolePermissionService.createPermissions(ROLE_ID, endpoints);

      var policyNameGenerator = nameGeneratorCaptor.getValue();
      assertThat(policyNameGenerator.apply(endpoint)).isEqualTo("GET access for role '%s' to '/foo/entities'", ROLE_ID);
      assertThat(newPolicyCaptor.getValue().get()).isEqualTo(new Policy().type(ROLE).name(policyName)
        .description("System generated policy for role: " + ROLE_ID)
        .rolePolicy(new RolePolicy().addRolesItem(new RolePolicyRole().id(ROLE_ID))));
    }

    @Test
    void positive_emptyEndpoints() {
      rolePermissionService.createPermissions(ROLE_ID, emptyList());
      verifyNoInteractions(keycloakAuthService, policyService, keycloakAuthService);
    }
  }

  @Nested
  @DisplayName("deletePermissions")
  class DeletePermissions {

    @Test
    void positive() {
      var policyName = "Policy for role: " + ROLE_ID;
      var policy = rolePolicy(policyName);
      var endpoint = endpoint("/foo/entities", GET);
      var endpoints = List.of(endpoint);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(policyService.getByNameAndType(policyName, ROLE)).thenReturn(policy);
      doNothing().when(keycloakAuthService).deletePermissions(eq(policy), eq(endpoints), nameGeneratorCaptor.capture());

      rolePermissionService.deletePermissions(ROLE_ID, endpoints);

      var policyNameGenerator = nameGeneratorCaptor.getValue();
      assertThat(policyNameGenerator.apply(endpoint)).isEqualTo("GET access for role '%s' to '/foo/entities'", ROLE_ID);
    }

    @Test
    void positive_emptyEndpoints() {
      rolePermissionService.deletePermissions(ROLE_ID, emptyList());
      verifyNoInteractions(keycloakAuthService, policyService, keycloakAuthService);
    }
  }
}
