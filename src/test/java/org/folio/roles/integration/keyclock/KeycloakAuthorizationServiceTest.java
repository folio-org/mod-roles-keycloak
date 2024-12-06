package org.folio.roles.integration.keyclock;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.PolicyUtils.POLICY_ID;
import static org.folio.roles.support.PolicyUtils.rolePolicy;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.support.TestUtils;
import org.folio.roles.utils.JsonHelper;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.PermissionsResource;
import org.keycloak.admin.client.resource.ResourcesResource;
import org.keycloak.admin.client.resource.ScopePermissionResource;
import org.keycloak.admin.client.resource.ScopePermissionsResource;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakAuthorizationServiceTest {

  private static final String SCOPE_ID = UUID.randomUUID().toString();
  private static final String RESOURCE_ID = UUID.randomUUID().toString();
  private static final String SCOPE_PERMISSION_ID = UUID.randomUUID().toString();

  private static final Function<Endpoint, String> PERMISSION_NAME_GENERATOR =
    endpoint -> String.format("%s access to %s", endpoint.getMethod(), endpoint.getPath());

  @InjectMocks private KeycloakAuthorizationService keycloakAuthService;

  @Mock private Response response;
  @Mock private ResourcesResource authResourcesClient;
  @Mock private PermissionsResource authPermissionsClient;
  @Mock private AuthorizationResource authorizationClient;
  @Mock private ScopePermissionResource scopePermissionClient;
  @Mock private ScopePermissionsResource scopePermissionsClient;
  @Mock private KeycloakAuthorizationClientProvider authResourceProvider;

  @Spy private final JsonHelper jsonHelper = new JsonHelper(OBJECT_MAPPER);
  @Captor private ArgumentCaptor<ScopePermissionRepresentation> scopePermissionCaptor;

  @BeforeEach
  void setUp() {
    Configurator.setLevel(KeycloakAuthorizationService.class, Level.DEBUG);
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static ResourceRepresentation resourceRepresentation() {
    var resourceRepresentation = new ResourceRepresentation();
    resourceRepresentation.setId(RESOURCE_ID);
    resourceRepresentation.setScopes(Set.of(scopeForGetMethod()));
    resourceRepresentation.setName("/foo/entities");
    return resourceRepresentation;
  }

  private static ScopeRepresentation scopeForGetMethod() {
    var scopeRepresentation = new ScopeRepresentation();
    scopeRepresentation.setId(SCOPE_ID);
    scopeRepresentation.setName("GET");
    return scopeRepresentation;
  }

  private static ScopeRepresentation scopeForPostMethod() {
    var scopeRepresentation = new ScopeRepresentation();
    scopeRepresentation.setId(SCOPE_ID);
    scopeRepresentation.setName("POST");
    return scopeRepresentation;
  }

  private static ScopePermissionRepresentation scopePermission() {
    var scopePermissionRepresentation = new ScopePermissionRepresentation();
    scopePermissionRepresentation.setId(SCOPE_PERMISSION_ID);
    scopePermissionRepresentation.setName("GET access to /foo/entities");
    scopePermissionRepresentation.setPolicies(Set.of(POLICY_ID.toString()));
    scopePermissionRepresentation.setResources(Set.of(RESOURCE_ID));
    scopePermissionRepresentation.setScopes(Set.of(SCOPE_ID));
    return scopePermissionRepresentation;
  }

  @Nested
  @DisplayName("createPermissions")
  class CreatePermissions {

    @DisplayName("positive_parameterized")
    @ParameterizedTest(name = "[{index}] responseStatus = {0}")
    @EnumSource(value = Status.class, names = {"CREATED", "CONFLICT"}, mode = Mode.INCLUDE)
    void positive_parameterized(Status responseStatus) {
      when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
      when(authorizationClient.resources()).thenReturn(authResourcesClient);
      when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
      when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      var resourceRepresentations = List.of(resourceRepresentation);
      when(authResourcesClient.find(path, null, null, null, null, 0, MAX_VALUE)).thenReturn(resourceRepresentations);
      when(scopePermissionsClient.create(scopePermissionCaptor.capture())).thenReturn(response);
      when(response.getStatusInfo()).thenReturn(responseStatus);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(response).close();
      verify(jsonHelper).asJsonStringSafe(resourceRepresentation);
      assertThat(scopePermissionCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(scopePermission());
    }

    @Test
    void negative_permissionIsNotCreated() {
      when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
      when(authorizationClient.resources()).thenReturn(authResourcesClient);
      when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
      when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      var resourceRepresentations = List.of(resourceRepresentation);
      when(authResourcesClient.find(path, null, null, null, null, 0, MAX_VALUE)).thenReturn(resourceRepresentations);
      when(scopePermissionsClient.create(scopePermissionCaptor.capture())).thenReturn(response);
      when(response.getStatusInfo()).thenReturn(Status.INTERNAL_SERVER_ERROR);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      assertThatThrownBy(() -> keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Error during scope-based permission creation in Keycloak. "
          + "Details: status = 500, message = Internal Server Error");

      verify(response).close();
      verify(jsonHelper).asJsonStringSafe(resourceRepresentation);
      assertThat(scopePermissionCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(scopePermission());
    }

    @Test
    void negative_resourceIsNotFound() {
      when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
      when(authorizationClient.resources()).thenReturn(authResourcesClient);
      when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
      when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);

      when(authResourcesClient.find("/foo/entities", null, null, null, null, 0, MAX_VALUE)).thenReturn(emptyList());

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      assertThatThrownBy(() -> keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Keycloak resource is not found by static path: /foo/entities");

      verifyNoInteractions(scopePermissionsClient);
    }

    @Test
    void negative_resourceIsFoundByInvalidPath() {
      when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
      when(authorizationClient.resources()).thenReturn(authResourcesClient);
      when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
      when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      resourceRepresentation.setName("/foo/entities/{id}");
      var resourceRepresentations = List.of(resourceRepresentation);
      when(authResourcesClient.find(path, null, null, null, null, 0, MAX_VALUE)).thenReturn(resourceRepresentations);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      assertThatThrownBy(() -> keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Keycloak resource is not found by static path: /foo/entities");

      verifyNoInteractions(scopePermissionsClient);
    }

    @Test
    void positive_resourceIsFoundWithInvalidScope() {
      when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
      when(authorizationClient.resources()).thenReturn(authResourcesClient);
      when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
      when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      resourceRepresentation.setScopes(Set.of(scopeForPostMethod()));
      var resourceRepresentations = List.of(resourceRepresentation);
      when(authResourcesClient.find(path, null, null, null, null, 0, MAX_VALUE)).thenReturn(resourceRepresentations);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(scopePermissionsClient);
      verify(jsonHelper).asJsonStringSafe(resourceRepresentation);
    }

    @Test
    void positive_policyIsNull() {
      var endpoints = List.of(endpoint());

      keycloakAuthService.createPermissions(null, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(authResourceProvider);
      verify(jsonHelper).asJsonString(null);
      verify(jsonHelper).asJsonString(endpoints);
    }

    @Test
    void positive_listOfEndpointIsEmpty() {
      var rolePolicy = rolePolicy();
      var endpoints = Collections.<Endpoint>emptyList();

      keycloakAuthService.createPermissions(rolePolicy, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(authResourceProvider);
      verify(jsonHelper).asJsonString(rolePolicy);
      verify(jsonHelper).asJsonString(endpoints);
    }
  }

  @Nested
  @DisplayName("deletePermissions")
  class DeletePermissions {

    @Test
    void positive() {
      when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
      when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
      when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);
      when(scopePermissionsClient.findByName("GET access to /foo/entities")).thenReturn(scopePermission());
      when(scopePermissionsClient.findById(SCOPE_PERMISSION_ID)).thenReturn(scopePermissionClient);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.deletePermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(scopePermissionClient).remove();
    }

    @Test
    void positive_permissionNotFound() {
      when(authResourceProvider.getAuthorizationClient()).thenReturn(authorizationClient);
      when(authorizationClient.permissions()).thenReturn(authPermissionsClient);
      when(authPermissionsClient.scope()).thenReturn(scopePermissionsClient);
      when(scopePermissionsClient.findByName("GET access to /foo/entities")).thenReturn(null);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.deletePermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(scopePermissionClient);
    }

    @Test
    void positive_policyIsNull() {
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.deletePermissions(null, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(authResourceProvider);
      verify(jsonHelper).asJsonString(null);
      verify(jsonHelper).asJsonString(endpoints);
    }

    @Test
    void positive_listOfEndpointIsEmpty() {
      var rolePolicy = rolePolicy();
      var endpoints = Collections.<Endpoint>emptyList();

      keycloakAuthService.deletePermissions(rolePolicy, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(authResourceProvider);
      verify(jsonHelper).asJsonString(rolePolicy);
      verify(jsonHelper).asJsonString(endpoints);
    }
  }
}
