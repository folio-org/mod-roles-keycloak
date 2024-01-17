package org.folio.roles.service.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.dto.PolicyType.USER;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.KeycloakUtils.keycloakUser;
import static org.folio.roles.support.PolicyUtils.userPolicy;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.UserPolicy;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationService;
import org.folio.roles.integration.keyclock.KeycloakUserService;
import org.folio.roles.service.policy.PolicyService;
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
class UserPermissionServiceTest {

  @InjectMocks private UserPermissionService userPermissionService;
  @Mock private PolicyService policyService;
  @Mock private KeycloakUserService keycloakUserService;
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
      var policyName = "Policy for user: " + USER_ID;
      var policy = userPolicy(policyName);
      var endpoint = endpoint("/foo/entities", GET);
      var endpoints = List.of(endpoint);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(policyService.getOrCreatePolicy(eq(policyName), eq(USER), newPolicyCaptor.capture())).thenReturn(policy);
      doNothing().when(keycloakAuthService).createPermissions(eq(policy), eq(endpoints), nameGeneratorCaptor.capture());

      userPermissionService.createPermissions(USER_ID, endpoints);

      var expectedPermissionName = String.format("GET access for user '%s' to '/foo/entities'", USER_ID);
      assertThat(nameGeneratorCaptor.getValue().apply(endpoint)).isEqualTo(expectedPermissionName);
      assertThat(newPolicyCaptor.getValue().get()).isEqualTo(new Policy().type(USER).name(policyName)
        .description("System generated policy for user: " + USER_ID)
        .userPolicy(new UserPolicy().users(List.of(USER_ID))));
    }
  }

  @Nested
  @DisplayName("deletePermissions")
  class DeletePermissions {

    @Test
    void positive() {
      var policyName = "Policy for user: " + USER_ID;
      var policy = userPolicy(policyName);
      var endpoint = endpoint("/foo/entities", GET);
      var endpoints = List.of(endpoint);

      when(keycloakUserService.getKeycloakUserByUserId(USER_ID)).thenReturn(keycloakUser());
      when(policyService.getByNameAndType(policyName, USER)).thenReturn(policy);
      doNothing().when(keycloakAuthService).deletePermissions(eq(policy), eq(endpoints), nameGeneratorCaptor.capture());

      userPermissionService.deletePermissions(USER_ID, endpoints);

      var policyNameGenerator = nameGeneratorCaptor.getValue();
      assertThat(policyNameGenerator.apply(endpoint)).isEqualTo("GET access for user '%s' to '/foo/entities'", USER_ID);
    }
  }
}
