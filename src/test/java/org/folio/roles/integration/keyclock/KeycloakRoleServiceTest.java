package org.folio.roles.integration.keyclock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.ROLE_NAME;
import static org.folio.roles.support.RoleUtils.keycloakRole;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.nio.charset.StandardCharsets;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.roles.support.TestUtils;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRoleServiceTest {

  @InjectMocks private KeycloakRoleService keycloakRoleService;

  @Mock private KeycloakAdminClient keycloakAdminClient;
  @Mock private FolioExecutionContext context;
  @Mock private KeycloakRoleMapper keycloakRoleMapper;

  @BeforeEach
  void setUp() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
  }

  @AfterEach
  void afterEach() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static HttpClientErrorException httpError(HttpStatus status) {
    return HttpClientErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, new byte[0],
      StandardCharsets.UTF_8);
  }

  @Nested
  @DisplayName("findByName")
  class FindByName {

    @Test
    void positive() {
      var role = role();
      var keycloakRole = keycloakRole();

      when(keycloakAdminClient.getRoleByName(TENANT_ID, ROLE_NAME)).thenReturn(keycloakRole);
      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role);

      var result = keycloakRoleService.findByName(role.getName());

      assertThat(result).contains(role);
    }

    @Test
    void positive_returnsEmptyRoleWhenNotFound() {
      when(keycloakAdminClient.getRoleByName(TENANT_ID, ROLE_NAME)).thenThrow(httpError(NOT_FOUND));

      var actual = keycloakRoleService.findByName(ROLE_NAME);

      assertThat(actual).isEmpty();
    }

    @Test
    void negative_keycloakApiExceptionIfUnauthorized() {
      when(keycloakAdminClient.getRoleByName(TENANT_ID, ROLE_NAME)).thenThrow(httpError(UNAUTHORIZED));

      assertThatThrownBy(() -> keycloakRoleService.findByName(ROLE_NAME))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to find role by name: %s", ROLE_NAME);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var keycloakRole = keycloakRole();
      var role = role().id(null);

      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);
      when(keycloakAdminClient.getRoleByName(TENANT_ID, ROLE_NAME)).thenReturn(keycloakRole);
      when(keycloakRoleMapper.toRole(keycloakRole)).thenReturn(role());

      var createdRole = keycloakRoleService.create(role);

      assertThat(createdRole).isEqualTo(role());
      verify(keycloakAdminClient).createRole(TENANT_ID, keycloakRole);
    }

    @Test
    void positive_creationFailed() {
      var keycloakRole = keycloakRole();
      var role = role();

      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);
      doThrow(httpError(CONFLICT)).when(keycloakAdminClient).createRole(TENANT_ID, keycloakRole);

      assertThatThrownBy(() -> keycloakRoleService.create(role))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessageContaining("Failed to create keycloak role: name = role1");
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      keycloakRoleService.deleteById(ROLE_ID);

      verify(keycloakAdminClient).deleteRoleById(TENANT_ID, ROLE_ID.toString());
    }

    @Test
    void positive_nullId() {
      keycloakRoleService.deleteById(null);

      verify(keycloakAdminClient).deleteRoleById(TENANT_ID, null);
    }

    @Test
    void negative_unauthorizedException() {
      org.mockito.Mockito.doThrow(httpError(UNAUTHORIZED))
        .when(keycloakAdminClient).deleteRoleById(TENANT_ID, ROLE_ID.toString());

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

      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);

      var result = keycloakRoleService.update(role);

      assertThat(result).isEqualTo(role);
      verify(keycloakAdminClient).updateRoleById(TENANT_ID, ROLE_ID.toString(), keycloakRole);
    }

    @Test
    void negative_unauthorizedException() {
      var role = role();
      var keycloakRole = keycloakRole();

      when(keycloakRoleMapper.toKeycloakRole(role)).thenReturn(keycloakRole);
      org.mockito.Mockito.doThrow(httpError(UNAUTHORIZED))
        .when(keycloakAdminClient).updateRoleById(TENANT_ID, ROLE_ID.toString(), keycloakRole);

      assertThatThrownBy(() -> keycloakRoleService.update(role))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to update role: %s", ROLE_ID);
    }
  }
}
