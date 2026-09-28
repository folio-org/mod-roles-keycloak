package org.folio.roles.integration.keyclock;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.PolicyUtils.POLICY_ID;
import static org.folio.roles.support.PolicyUtils.rolePolicy;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.support.TestUtils;
import org.folio.roles.utils.JsonHelper;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakAuthorizationServiceTest {

  private static final String CLIENT_UUID = UUID.randomUUID().toString();
  private static final String SCOPE_ID = UUID.randomUUID().toString();
  private static final String RESOURCE_ID = UUID.randomUUID().toString();
  private static final String SCOPE_PERMISSION_ID = UUID.randomUUID().toString();

  private static final String RESOURCE_ID_2 = UUID.randomUUID().toString();
  private static final String SCOPE_ID_2 = UUID.randomUUID().toString();
  private static final String SCOPE_PERMISSION_ID_2 = UUID.randomUUID().toString();

  private static final Function<Endpoint, String> PERMISSION_NAME_GENERATOR =
    endpoint -> String.format("%s access to %s", endpoint.getMethod(), endpoint.getPath());

  @InjectMocks
  private KeycloakAuthorizationService keycloakAuthService;

  @Mock
  private KeycloakAdminClient keycloakAdminClient;
  @Mock
  private KeycloakClientService keycloakClientService;
  @Mock
  private FolioExecutionContext context;
  @Mock
  private KeycloakPermissionsExecutor permissionsExecutor;

  @Spy
  private final JsonHelper jsonHelper = new JsonHelper(OBJECT_MAPPER);
  @Captor
  private ArgumentCaptor<ScopePermissionRepresentation> scopePermissionCaptor;

  @BeforeEach
  void setUp() {
    Configurator.setLevel(KeycloakAuthorizationService.class, Level.DEBUG);
  }

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static HttpClientErrorException httpError(HttpStatus status) {
    return HttpClientErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, new byte[0],
      StandardCharsets.UTF_8);
  }

  private void stubContextAndClient() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(keycloakClientService.getLoginClient()).thenReturn(loginClient());
  }

  private void stubExecutorRunsAction() {
    doAnswer(inv -> {
      List<Endpoint> eps = inv.getArgument(0);
      Consumer<Endpoint> action = inv.getArgument(1);
      eps.forEach(action);
      return null;
    }).when(permissionsExecutor).execute(any(), any());
  }

  private static ClientRepresentation loginClient() {
    var client = new ClientRepresentation();
    client.setId(CLIENT_UUID);
    return client;
  }

  private static ResourceRepresentation resourceRepresentation() {
    var resourceRepresentation = new ResourceRepresentation();
    resourceRepresentation.setId(RESOURCE_ID);
    resourceRepresentation.setScopes(Set.of(scopeForGetMethod()));
    resourceRepresentation.setName("/foo/entities");
    return resourceRepresentation;
  }

  /**
   * Creates a resource representation with the given path, method, resource ID, and scope ID.
   */
  private static ResourceRepresentation resourceRepresentation(
    String path, String method, String resourceId, String scopeId) {
    var resourceRepresentation = new ResourceRepresentation();
    resourceRepresentation.setId(resourceId);
    var scopeRepresentation = new ScopeRepresentation();
    scopeRepresentation.setId(scopeId);
    scopeRepresentation.setName(method);
    resourceRepresentation.setScopes(Set.of(scopeRepresentation));
    resourceRepresentation.setName(path);
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

    @Test
    void positive_permissionCreated() {
      stubContextAndClient();
      stubExecutorRunsAction();

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      when(keycloakAdminClient.findAuthResources(TENANT_ID, CLIENT_UUID, path, 0, MAX_VALUE))
        .thenReturn(List.of(resourceRepresentation));

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(keycloakAdminClient).createScopePermission(eq(TENANT_ID), eq(CLIENT_UUID),
        scopePermissionCaptor.capture());
      verify(jsonHelper).asJsonStringSafe(resourceRepresentation);
      assertThat(scopePermissionCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(scopePermission());
    }

    @Test
    void positive_conflictIsSwallowed() {
      stubContextAndClient();
      stubExecutorRunsAction();

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      when(keycloakAdminClient.findAuthResources(TENANT_ID, CLIENT_UUID, path, 0, MAX_VALUE))
        .thenReturn(List.of(resourceRepresentation));
      doThrow(httpError(CONFLICT)).when(keycloakAdminClient)
        .createScopePermission(eq(TENANT_ID), eq(CLIENT_UUID), scopePermissionCaptor.capture());

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(jsonHelper).asJsonStringSafe(resourceRepresentation);
      assertThat(scopePermissionCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(scopePermission());
    }

    @Test
    void createPermissions_multipleEndpoints() {
      stubContextAndClient();
      stubExecutorRunsAction();

      // Each path returns a resource with a distinct ID to reflect real Keycloak behaviour
      when(keycloakAdminClient.findAuthResources(eq(TENANT_ID), eq(CLIENT_UUID), eq("/foo/entities"), eq(0),
        eq(MAX_VALUE)))
        .thenReturn(List.of(resourceRepresentation("/foo/entities", "GET", RESOURCE_ID, SCOPE_ID)));
      when(keycloakAdminClient.findAuthResources(eq(TENANT_ID), eq(CLIENT_UUID), eq("/bar/items"), eq(0),
        eq(MAX_VALUE)))
        .thenReturn(List.of(resourceRepresentation("/bar/items", "GET", RESOURCE_ID_2, SCOPE_ID_2)));

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET), endpoint("/bar/items", GET));

      keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(jsonHelper, times(2)).asJsonStringSafe(any(ResourceRepresentation.class));
      verify(keycloakAdminClient, times(2)).createScopePermission(eq(TENANT_ID), eq(CLIENT_UUID),
        scopePermissionCaptor.capture());
      assertThat(scopePermissionCaptor.getAllValues())
        .extracting(ScopePermissionRepresentation::getName)
        .containsExactlyInAnyOrder("GET access to /foo/entities", "GET access to /bar/items");
      // Verify each permission carries the correct (distinct) resource ID
      assertThat(scopePermissionCaptor.getAllValues())
        .anySatisfy(p -> assertThat(p.getResources()).containsExactly(RESOURCE_ID))
        .anySatisfy(p -> assertThat(p.getResources()).containsExactly(RESOURCE_ID_2));
    }

    @Test
    void negative_permissionIsNotCreated() {
      stubContextAndClient();
      stubExecutorRunsAction();

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      when(keycloakAdminClient.findAuthResources(TENANT_ID, CLIENT_UUID, path, 0, MAX_VALUE))
        .thenReturn(List.of(resourceRepresentation));
      doThrow(httpError(INTERNAL_SERVER_ERROR)).when(keycloakAdminClient)
        .createScopePermission(eq(TENANT_ID), eq(CLIENT_UUID), scopePermissionCaptor.capture());

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      assertThatThrownBy(() -> keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Error during scope-based permission creation in Keycloak. "
          + "Details: status = 500, message = Internal Server Error");

      verify(jsonHelper).asJsonStringSafe(resourceRepresentation);
      assertThat(scopePermissionCaptor.getValue())
        .usingRecursiveComparison()
        .ignoringFields("id")
        .isEqualTo(scopePermission());
    }

    @Test
    void negative_resourceIsNotFound() {
      stubContextAndClient();
      stubExecutorRunsAction();

      when(keycloakAdminClient.findAuthResources(TENANT_ID, CLIENT_UUID, "/foo/entities", 0, MAX_VALUE))
        .thenReturn(emptyList());

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      assertThatThrownBy(() -> keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Keycloak resource is not found by static path: /foo/entities");
    }

    @Test
    void negative_resourceIsFoundByInvalidPath() {
      stubContextAndClient();
      stubExecutorRunsAction();

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      resourceRepresentation.setName("/foo/entities/{id}");
      when(keycloakAdminClient.findAuthResources(TENANT_ID, CLIENT_UUID, path, 0, MAX_VALUE))
        .thenReturn(List.of(resourceRepresentation));

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      assertThatThrownBy(() -> keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Keycloak resource is not found by static path: /foo/entities");
    }

    @Test
    void positive_resourceIsFoundWithInvalidScope() {
      stubContextAndClient();
      stubExecutorRunsAction();

      var path = "/foo/entities";
      var resourceRepresentation = resourceRepresentation();
      resourceRepresentation.setScopes(Set.of(scopeForPostMethod()));
      when(keycloakAdminClient.findAuthResources(TENANT_ID, CLIENT_UUID, path, 0, MAX_VALUE))
        .thenReturn(List.of(resourceRepresentation));

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.createPermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(jsonHelper).asJsonStringSafe(resourceRepresentation);
    }

    @Test
    void positive_policyIsNull() {
      var endpoints = List.of(endpoint());

      keycloakAuthService.createPermissions(null, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(keycloakAdminClient, keycloakClientService, permissionsExecutor);
      verify(jsonHelper).asJsonString(null);
      verify(jsonHelper).asJsonString(endpoints);
    }

    @Test
    void positive_listOfEndpointIsEmpty() {
      var rolePolicy = rolePolicy();
      var endpoints = Collections.<Endpoint>emptyList();

      keycloakAuthService.createPermissions(rolePolicy, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(keycloakAdminClient, keycloakClientService, permissionsExecutor);
      verify(jsonHelper).asJsonString(rolePolicy);
      verify(jsonHelper).asJsonString(endpoints);
    }
  }

  @Nested
  @DisplayName("deletePermissions")
  class DeletePermissions {

    @Test
    void positive() {
      stubContextAndClient();
      stubExecutorRunsAction();

      when(keycloakAdminClient.findScopePermissionByName(TENANT_ID, CLIENT_UUID, "GET access to /foo/entities"))
        .thenReturn(scopePermission());

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.deletePermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(keycloakAdminClient).deleteScopePermissionById(TENANT_ID, CLIENT_UUID, SCOPE_PERMISSION_ID);
    }

    @Test
    void positive_permissionNotFound() {
      stubContextAndClient();
      stubExecutorRunsAction();

      when(keycloakAdminClient.findScopePermissionByName(TENANT_ID, CLIENT_UUID, "GET access to /foo/entities"))
        .thenReturn(null);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.deletePermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(keycloakAdminClient).findScopePermissionByName(TENANT_ID, CLIENT_UUID, "GET access to /foo/entities");
    }

    @Test
    void positive_permissionNotFoundOn404() {
      stubContextAndClient();
      stubExecutorRunsAction();

      when(keycloakAdminClient.findScopePermissionByName(TENANT_ID, CLIENT_UUID, "GET access to /foo/entities"))
        .thenThrow(httpError(NOT_FOUND));

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.deletePermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(keycloakAdminClient).findScopePermissionByName(TENANT_ID, CLIENT_UUID, "GET access to /foo/entities");
    }

    @Test
    void positive_policyIsNull() {
      var endpoints = List.of(endpoint("/foo/entities", GET));

      keycloakAuthService.deletePermissions(null, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(keycloakAdminClient, keycloakClientService, permissionsExecutor);
      verify(jsonHelper).asJsonString(null);
      verify(jsonHelper).asJsonString(endpoints);
    }

    @Test
    void positive_listOfEndpointIsEmpty() {
      var rolePolicy = rolePolicy();
      var endpoints = Collections.<Endpoint>emptyList();

      keycloakAuthService.deletePermissions(rolePolicy, endpoints, PERMISSION_NAME_GENERATOR);

      verifyNoInteractions(keycloakAdminClient, keycloakClientService, permissionsExecutor);
      verify(jsonHelper).asJsonString(rolePolicy);
      verify(jsonHelper).asJsonString(endpoints);
    }

    @Test
    void deletePermissions_multipleEndpoints() {
      stubContextAndClient();
      stubExecutorRunsAction();

      var fooPermission = new ScopePermissionRepresentation();
      fooPermission.setId(SCOPE_PERMISSION_ID);
      fooPermission.setName("GET access to /foo/entities");

      var barPermission = new ScopePermissionRepresentation();
      barPermission.setId(SCOPE_PERMISSION_ID_2);
      barPermission.setName("GET access to /bar/items");

      when(keycloakAdminClient.findScopePermissionByName(TENANT_ID, CLIENT_UUID, "GET access to /foo/entities"))
        .thenReturn(fooPermission);
      when(keycloakAdminClient.findScopePermissionByName(TENANT_ID, CLIENT_UUID, "GET access to /bar/items"))
        .thenReturn(barPermission);

      var policy = rolePolicy();
      var endpoints = List.of(endpoint("/foo/entities", GET), endpoint("/bar/items", GET));

      keycloakAuthService.deletePermissions(policy, endpoints, PERMISSION_NAME_GENERATOR);

      verify(keycloakAdminClient).deleteScopePermissionById(TENANT_ID, CLIENT_UUID, SCOPE_PERMISSION_ID);
      verify(keycloakAdminClient).deleteScopePermissionById(TENANT_ID, CLIENT_UUID, SCOPE_PERMISSION_ID_2);
    }
  }
}
