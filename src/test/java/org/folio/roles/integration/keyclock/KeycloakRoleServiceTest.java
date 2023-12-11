package org.folio.roles.integration.keyclock;

import static org.folio.roles.support.KeycloakUtils.ACCESS_TOKEN;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.keycloakRole;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.RoleUtils.role2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.integration.keyclock.client.RoleClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.integration.keyclock.model.KeycloakRole;
import org.folio.roles.mapper.RoleMapper;
import org.folio.roles.mapper.RoleMapperImpl;
import org.folio.roles.support.RoleUtils;
import org.folio.roles.support.TestConstants;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRoleServiceTest {

  @Mock private RoleClient roleClient;
  @Mock private KeycloakAccessTokenService tokenService;
  @Mock private FolioExecutionContext context;
  @Spy private RoleMapper roleMapper = new RoleMapperImpl();

  @InjectMocks private KeycloakRoleService roleService;

  @BeforeEach
  void setUp() {
    when(tokenService.getToken()).thenReturn(ACCESS_TOKEN);
    when(context.getTenantId()).thenReturn(TestConstants.TENANT_ID);
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(roleClient, tokenService, context);
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    void positive_returns_role() {
      var role = role();
      var keycloakRole = keycloakRole();
      var token = tokenService.getToken();

      when(roleClient.findById(anyString(), eq(token), eq(role.getId()))).thenReturn(keycloakRole);

      var actual = roleService.findById(role.getId());

      assertEquals(actual, role());
    }

    @Test
    void negative_throws_keycloak_api_exception_while_feign_exception() {
      var token = tokenService.getToken();

      when(roleClient.findById(anyString(), eq(token), any(UUID.class))).thenThrow(FeignException.class);

      assertThrows(KeycloakApiException.class, () -> roleService.findById(ROLE_ID));
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive_returns_optional_of_created_roles() {
      var keycloakRole = keycloakRole();
      var role = role();
      var roles = new Roles().addRolesItem(role);
      roles.setTotalRecords(roles.getRoles().size());
      var token = tokenService.getToken();

      when(roleClient.findByName(anyString(), eq(token), eq(keycloakRole.getName()))).thenReturn(keycloakRole);

      var createdRoles = roleService.createSafe(role);

      assertEquals(createdRoles.get(), role);
      verify(roleClient, times(1)).findByName(anyString(), anyString(), anyString());
      verify(roleClient, times(1)).create(anyString(), anyString(), any());
    }

    @Test
    void negative_returns_empty_optional_if_creation_failed() {
      var role = role();

      doThrow(FeignException.Conflict.class).when(roleClient).create(anyString(), any(), any());

      var result = roleService.createSafe(role);
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive_returns_all_if_null_parameters() {
      var role = role();
      var role2 = role2();
      var keycloakRole = RoleUtils.keycloakRole(role);
      var keycloakRole2 = RoleUtils.keycloakRole(role2);
      var token = tokenService.getToken();

      when(roleClient.find(anyString(), eq(token), any(), any(), any())).thenReturn(
        List.of(keycloakRole, keycloakRole2));

      var roles = roleService.search(null, null, null);

      assertEquals(List.of(role, role2), roles.getRoles());
      assertEquals(2, roles.getTotalRecords());
    }

    @Test
    void positive_returns_empty_result() {
      var token = tokenService.getToken();

      when(roleClient.find(anyString(), eq(token), anyInt(), anyInt(), anyString())).thenReturn(List.of());

      assertEquals(roleService.search("test-search", 0, 10), new Roles().totalRecords(0));
    }

    @Test
    void negative_throws_api_exception() {
      doThrow(FeignException.class).when(roleClient).find(anyString(), any(), any(), any(), any());

      assertThrows(KeycloakApiException.class, () -> roleService.search(null, null, null));
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      roleService.deleteById(ROLE_ID);

      var token = tokenService.getToken();
      verify(roleClient).deleteById(anyString(), eq(token), eq(ROLE_ID));
    }

    @Test
    void negative_throws_api_exception() {
      var token = tokenService.getToken();

      doThrow(FeignException.class).when(roleClient).deleteById(anyString(), eq(token), eq(ROLE_ID));

      assertThrows(KeycloakApiException.class, () -> roleService.deleteById(ROLE_ID));
      verify(roleClient).deleteById(anyString(), any(), any());
    }
  }

  @Nested
  @DisplayName("updateById")
  class UpdateById {

    @Test
    void positive() {
      roleService.update(role());

      var keycloakRole = keycloakRole();
      var token = tokenService.getToken();
      verify(roleClient).updateById(anyString(), eq(token), eq(ROLE_ID), eq(keycloakRole));
    }

    @Test
    void negative_throws_api_exception() {
      var token = tokenService.getToken();
      var role = role();

      doThrow(FeignException.class).when(roleClient)
        .updateById(anyString(), eq(token), eq(ROLE_ID), any(KeycloakRole.class));

      assertThrows(KeycloakApiException.class, () -> roleService.update(role));
      verify(roleClient).updateById(anyString(), anyString(), any(), any());
    }
  }
}
