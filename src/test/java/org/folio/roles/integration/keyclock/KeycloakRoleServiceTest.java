package org.folio.roles.integration.keyclock;

import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.ROLE_NAME;
import static org.folio.roles.support.RoleUtils.keycloakRole;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.RoleUtils.role2;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRoleServiceTest {

  @InjectMocks private KeycloakRoleService keycloakRoleService;

  @Mock private Keycloak keycloak;
  @Mock private FolioExecutionContext context;
  @Mock private KeycloakRoleMapper keycloakRoleMapper;
  @Mock(answer = RETURNS_DEEP_STUBS) private RealmResource realmResource;

  @BeforeEach
  void setUp() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
  }

  @AfterEach
  void afterEach() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    void positive_roleFound() {
      var role = role();
      var keycloakRole = keycloakRole();

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById().getRole(ROLE_ID.toString())).thenReturn(keycloakRole);
      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role);

      var actual = keycloakRoleService.findById(ROLE_ID);

      assertThat(actual).contains(role);
      verify(realmResource, atLeastOnce()).rolesById();
    }

    @Test
    void negative_notFoundException() {
      var exception = new NotFoundException(new ServerResponse(null, 404, new Headers<>()));

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById().getRole(ROLE_ID.toString())).thenThrow(exception);

      var actual = keycloakRoleService.findById(ROLE_ID);

      assertThat(actual).isEmpty();
      verify(realmResource, atLeastOnce()).rolesById();
    }

    @Test
    void negative_keycloakApiExceptionIfUnauthorized() {
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById().getRole(ROLE_ID.toString())).thenThrow(exception);

      assertThatThrownBy(() -> keycloakRoleService.getById(ROLE_ID))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to find role: id = %s", ROLE_ID);

      verify(realmResource, atLeastOnce()).rolesById();
    }
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    void positive_roleFound() {
      var role = role();
      var keycloakRole = keycloakRole();

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById().getRole(ROLE_ID.toString())).thenReturn(keycloakRole);
      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role);

      var actual = keycloakRoleService.getById(ROLE_ID);

      assertThat(actual).isEqualTo(role);
      verify(realmResource, atLeastOnce()).rolesById();
    }

    @Test
    void negative_notFoundException() {
      var exception = new NotFoundException(new ServerResponse(null, 404, new Headers<>()));

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById().getRole(ROLE_ID.toString())).thenThrow(exception);

      assertThatThrownBy(() -> keycloakRoleService.getById(ROLE_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Failed to find role: id = %s", ROLE_ID);

      verify(realmResource, atLeastOnce()).rolesById();
    }

    @Test
    void negative_keycloakApiExceptionIfUnauthorized() {
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById().getRole(ROLE_ID.toString())).thenThrow(exception);

      assertThatThrownBy(() -> keycloakRoleService.getById(ROLE_ID))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to find role: id = %s", ROLE_ID);

      verify(realmResource, atLeastOnce()).rolesById();
    }
  }

  @Nested
  @DisplayName("findByName")
  class FindByName {

    @Test
    void positive() {
      var role = role();
      var keycloakRole = keycloakRole();

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles().get(ROLE_NAME).toRepresentation()).thenReturn(keycloakRole);
      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role);

      var result = keycloakRoleService.findByName(role.getName());

      assertThat(result).contains(role);
      verify(realmResource, atLeastOnce()).roles();
    }

    @Test
    void positive_returnsEmptyRoleWhenNotFound() {
      var exception = new NotFoundException(new ServerResponse(null, 404, new Headers<>()));
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles().get(ROLE_NAME).toRepresentation()).thenThrow(exception);

      var actual = keycloakRoleService.findByName(ROLE_NAME);

      assertThat(actual).isEmpty();
      verify(realmResource, atLeastOnce()).roles();
    }

    @Test
    void negative_keycloakApiExceptionIfUnauthorized() {
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles().get(ROLE_NAME).toRepresentation()).thenThrow(exception);

      assertThatThrownBy(() -> keycloakRoleService.findByName(ROLE_NAME))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to find role by name: %s", ROLE_NAME);

      verify(realmResource, atLeastOnce()).roles();
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var keycloakRole = keycloakRole();
      var role = role().id(null);
      var rolesResource = mock(RolesResource.class, RETURNS_DEEP_STUBS);

      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles()).thenReturn(rolesResource);
      doNothing().when(rolesResource).create(keycloakRole);

      when(rolesResource.get(ROLE_NAME).toRepresentation()).thenReturn(keycloakRole);
      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role());

      var createdRole = keycloakRoleService.create(role);

      assertThat(createdRole).isEqualTo(role());
      verify(rolesResource, atLeastOnce()).get(ROLE_NAME);
    }

    @Test
    void positive_creationFailed() {
      var keycloakRole = keycloakRole();
      var role = role();
      var rolesResource = mock(RolesResource.class);
      var response = new ServerResponse(null, CONFLICT.getStatusCode(), new Headers<>());
      var conflictException = new WebApplicationException(response);

      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles()).thenReturn(rolesResource);
      doThrow(conflictException).when(rolesResource).create(keycloakRole);

      assertThatThrownBy(() -> keycloakRoleService.create(role))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessageContaining("Failed to create keycloak role: name = role1");
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive() {
      var role = role();
      var role2 = role2();
      var keycloakRole = keycloakRole(role);
      var keycloakRole2 = keycloakRole(role2);
      var query = "test-role";

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role);
      when(keycloakRoleMapper.toRole(keycloakRole2)).thenReturn(role2);
      when(realmResource.roles().list(query, 1, 10, false)).thenReturn(List.of(keycloakRole, keycloakRole2));

      var roles = keycloakRoleService.search(query, 1, 10);

      assertThat(roles).isEqualTo(PageResult.asSinglePage(role, role2));
      verify(realmResource, atLeastOnce()).roles();
    }

    @Test
    void positive_nullQuery() {
      var role = role();
      var role2 = role2();
      var keycloakRole = keycloakRole(role);
      var keycloakRole2 = keycloakRole(role2);

      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role);
      when(keycloakRoleMapper.toRole(keycloakRole2)).thenReturn(role2);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles().list(null, null, null, false)).thenReturn(List.of(keycloakRole, keycloakRole2));

      var roles = keycloakRoleService.search(null, null, null);

      assertThat(roles).isEqualTo(PageResult.asSinglePage(role, role2));
      verify(realmResource, atLeastOnce()).roles();
    }

    @Test
    void positive_returns_empty_result() {
      var query = "test-role";
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles().list(query, 0, 10, false)).thenReturn(emptyList());

      var result = keycloakRoleService.search(query, 0, 10);

      assertThat(result).isEqualTo(PageResult.empty());
      verify(realmResource, atLeastOnce()).roles();
    }

    @Test
    void negative_notAuthorizedException() {
      var query = "test-role";
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.roles().list(query, 0, 10, false)).thenThrow(exception);

      assertThatThrownBy(() -> keycloakRoleService.search(query, 0, 10))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to search roles by query: %s", query);

      verify(realmResource, atLeastOnce()).roles();
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      var rolesByIdResource = mock(RoleByIdResource.class);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById()).thenReturn(rolesByIdResource);

      keycloakRoleService.deleteById(ROLE_ID);

      verify(rolesByIdResource).deleteRole(ROLE_ID.toString());
    }

    @Test
    void positive_nullId() {
      var rolesByIdResource = mock(RoleByIdResource.class);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById()).thenReturn(rolesByIdResource);

      keycloakRoleService.deleteById(null);

      verify(rolesByIdResource).deleteRole(null);
    }

    @Test
    void negative_unauthorizedException() {
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));

      var rolesByIdResource = mock(RoleByIdResource.class);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById()).thenReturn(rolesByIdResource);
      doThrow(exception).when(rolesByIdResource).deleteRole(ROLE_ID.toString());

      assertThatThrownBy(() -> keycloakRoleService.deleteById(ROLE_ID))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to delete role: %s", ROLE_ID);
    }
  }

  @Nested
  @DisplayName("updateById")
  class UpdateById {

    @Test
    void positive() {
      var role = role();
      var keycloakRole = keycloakRole();
      var rolesByIdResource = mock(RoleByIdResource.class);

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById()).thenReturn(rolesByIdResource);
      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);
      doNothing().when(rolesByIdResource).updateRole(ROLE_ID.toString(), keycloakRole);

      var result = keycloakRoleService.update(role);

      assertThat(result).isEqualTo(role);
    }

    @Test
    void negative_unauthorizedException() {
      var role = role();
      var keycloakRole = keycloakRole();
      var rolesByIdResource = mock(RoleByIdResource.class);
      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.rolesById()).thenReturn(rolesByIdResource);
      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);
      doThrow(exception).when(rolesByIdResource).updateRole(ROLE_ID.toString(), keycloakRole);

      assertThatThrownBy(() -> keycloakRoleService.update(role))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to update role: %s", ROLE_ID);
    }
  }
}
