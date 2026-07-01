package org.folio.roles.service.migration;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.permissions.Permissions;
import org.folio.roles.integration.permissions.PermissionsClient;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class UserPermissionsLoaderTest {

  @InjectMocks UserPermissionsLoader userPermissionsLoader;
  @Mock private KeycloakAdminClient keycloakAdminClient;
  @Mock private PermissionsClient permissionsClient;
  @Mock private FolioExecutionContext folioExecutionContext;

  @Mock(answer = RETURNS_DEEP_STUBS) private KeycloakConfigurationProperties configurationProperties;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void loadUserPermissions_positive() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(configurationProperties.getMigration().getUsersBatchSize()).thenReturn(1);

    when(keycloakAdminClient.listUsers(TENANT_ID, 0, 1)).thenReturn(List.of(keycloakUser(USER_ID)));
    when(keycloakAdminClient.listUsers(TENANT_ID, 1, 1)).thenReturn(List.of(keycloakUser(null)));
    when(keycloakAdminClient.listUsers(TENANT_ID, 2, 1)).thenReturn(emptyList());

    var permissions = List.of("foo.item.get", "foo.item.post");
    var permissionNames = new Permissions().permissionNames(permissions).totalRecords(2);
    when(permissionsClient.getUserPermissions(USER_ID, "userId", true)).thenReturn(Optional.of(permissionNames));

    var result = userPermissionsLoader.loadUserPermissions();

    var generatedRoleName = "aeed42c8e8015379434a14fc3414e1209d7ccd59";
    var userPermissions = new UserPermissions().userId(USER_ID).permissions(permissions).roleName(generatedRoleName);
    assertThat(result).containsExactly(userPermissions);

    verify(configurationProperties, atLeastOnce()).getMigration();
  }

  @Test
  void loadUserPermissions_positive_permissionNamesNull() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(configurationProperties.getMigration().getUsersBatchSize()).thenReturn(1);

    when(keycloakAdminClient.listUsers(TENANT_ID, 0, 1)).thenReturn(List.of(keycloakUser(USER_ID)));
    when(keycloakAdminClient.listUsers(TENANT_ID, 1, 1)).thenReturn(emptyList());

    when(permissionsClient.getUserPermissions(USER_ID, "userId", true)).thenReturn(Optional.of(new Permissions()));

    var result = userPermissionsLoader.loadUserPermissions();

    assertThat(result).isEmpty();

    verify(configurationProperties, atLeastOnce()).getMigration();
  }

  @Test
  void loadUserPermissions_negative_usersNotFound() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT_ID);
    when(configurationProperties.getMigration().getUsersBatchSize()).thenReturn(1);

    when(keycloakAdminClient.listUsers(TENANT_ID, 0, 1)).thenReturn(emptyList());

    var result = userPermissionsLoader.loadUserPermissions();

    assertThat(result).isEmpty();
    verify(configurationProperties, atLeastOnce()).getMigration();
  }

  private static UserRepresentation keycloakUser(UUID folioUserId) {
    var userRepresentation = new UserRepresentation();
    userRepresentation.setId(UUID.randomUUID().toString());

    if (folioUserId != null) {
      userRepresentation.setAttributes(Map.of("user_id", List.of(folioUserId.toString())));
    }

    return userRepresentation;
  }
}
