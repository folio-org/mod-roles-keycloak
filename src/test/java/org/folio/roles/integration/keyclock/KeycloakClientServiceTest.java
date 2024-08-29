package org.folio.roles.integration.keyclock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties.Login;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakClientServiceTest {

  private static final String CLIENT_ID = UUID.randomUUID().toString();
  private static final String LOGIN_CLIENT_SUFFIX = "-test-login-app";

  @InjectMocks private KeycloakClientService service;

  @Mock private Keycloak keycloak;
  @Mock private FolioExecutionContext context;
  @Mock private KeycloakConfigurationProperties properties;
  @Mock(answer = RETURNS_DEEP_STUBS) private RealmResource realmResource;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static ClientRepresentation loginKeycloakClient() {
    var clientRepresentation = new ClientRepresentation();
    clientRepresentation.setId(CLIENT_ID);
    clientRepresentation.setClientId(TENANT_ID + LOGIN_CLIENT_SUFFIX);
    return clientRepresentation;
  }

  private static ClientRepresentation otherKeycloakClient() {
    var clientRepresentation = new ClientRepresentation();
    clientRepresentation.setId(UUID.randomUUID().toString());
    clientRepresentation.setClientId("test keycloak client");
    return clientRepresentation;
  }

  @Nested
  @DisplayName("getLoginClient")
  class GetLoginClient {

    @BeforeEach
    void setUp() {
      var loginPropertiesMock = mock(Login.class);
      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(properties.getLogin()).thenReturn(loginPropertiesMock);
      when(loginPropertiesMock.getClientNameSuffix()).thenReturn(LOGIN_CLIENT_SUFFIX);
    }

    @Test
    void positive() {
      var clientName = TENANT_ID + LOGIN_CLIENT_SUFFIX;
      var clientRepresentation = loginKeycloakClient();

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.clients().findByClientId(clientName)).thenReturn(List.of(clientRepresentation));

      var result = service.getLoginClient();

      assertThat(result).isEqualTo(clientRepresentation);
      verify(realmResource, atLeastOnce()).clients();
    }

    @Test
    void negative_multipleClientsFound() {
      var clientName = TENANT_ID + LOGIN_CLIENT_SUFFIX;
      var loginClient = loginKeycloakClient();
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.clients().findByClientId(clientName)).thenReturn(List.of(loginClient, otherKeycloakClient()));

      var result = service.getLoginClient();

      assertThat(result).isEqualTo(loginClient);
      verify(realmResource, atLeastOnce()).clients();
    }

    @Test
    void negative_invalidClientFoundByName() {
      var clientName = TENANT_ID + LOGIN_CLIENT_SUFFIX;

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.clients().findByClientId(clientName)).thenReturn(List.of(otherKeycloakClient()));

      assertThatThrownBy(() -> service.getLoginClient())
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Client '%s' not found.", clientName);

      verify(realmResource, atLeastOnce()).clients();
    }

    @Test
    void negative_nothingFound() {
      var clientName = TENANT_ID + LOGIN_CLIENT_SUFFIX;
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.clients().findByClientId(clientName)).thenReturn(List.of(otherKeycloakClient()));

      assertThatThrownBy(() -> service.getLoginClient())
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Client '%s' not found.", clientName);

      verify(realmResource, atLeastOnce()).clients();
    }
  }
}
