package org.folio.roles.integration.keyclock;

import static org.folio.roles.support.KeycloakUtils.ACCESS_TOKEN;
import static org.folio.roles.support.TestConstants.LOGIN_CLIENT_SUFFIX;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.roles.integration.keyclock.client.ClientManagerClient;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties.Login;
import org.folio.roles.integration.keyclock.model.KeycloakClient;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.exception.NotFoundException;
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

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakClientServiceTest {

  private static final String EXPECTED_ID = "expected-id";

  @Mock private ClientManagerClient client;
  @Mock private KeycloakConfigurationProperties properties;
  @Mock private FolioExecutionContext context;
  @Mock private KeycloakAccessTokenService tokenService;

  @InjectMocks private KeycloakClientService service;

  @Nested
  @DisplayName("findAndCacheClientIdUsingProperties")
  class FindAndCacheClientIdUsingProperties {

    @BeforeEach
    void setUp() {
      var loginPropertiesMock = mock(Login.class);
      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(properties.getLogin()).thenReturn(loginPropertiesMock);
      when(loginPropertiesMock.getClientNameSuffix()).thenReturn(LOGIN_CLIENT_SUFFIX);
      when(tokenService.getToken()).thenReturn(ACCESS_TOKEN);
    }

    @AfterEach
    void afterEach() {
      verifyNoMoreInteractions(context, client, tokenService, properties);
    }

    @Test
    void positive() {
      var clientName = TENANT_ID + LOGIN_CLIENT_SUFFIX;
      var tenantId = context.getTenantId();
      var token = tokenService.getToken();
      var keyClientResponse = new KeycloakClient();
      keyClientResponse.setId(EXPECTED_ID);

      when(client.findClientsByClientId(token, tenantId, clientName)).thenReturn(List.of(keyClientResponse));

      var result = service.findAndCacheLoginClientUuid();
      assertEquals(EXPECTED_ID, result);
    }

    @Test
    void negative_multipleClientsFound() {
      var clientName = TENANT_ID + LOGIN_CLIENT_SUFFIX;
      var tenantId = context.getTenantId();
      var token = tokenService.getToken();
      var keyClientResponse1 = new KeycloakClient();
      keyClientResponse1.setId(EXPECTED_ID);
      var keyClientResponse2 = new KeycloakClient();
      keyClientResponse2.setId("another-id");

      when(client.findClientsByClientId(token, tenantId, clientName)).thenReturn(
        List.of(keyClientResponse1, keyClientResponse2));

      assertThrows(IllegalStateException.class, () -> service.findAndCacheLoginClientUuid());
    }

    @Test
    void negative_notFound() {
      var clientName = TENANT_ID + LOGIN_CLIENT_SUFFIX;
      var tenantId = context.getTenantId();
      var token = tokenService.getToken();

      when(client.findClientsByClientId(token, tenantId, clientName)).thenReturn(List.of());

      assertThrows(NotFoundException.class, () -> service.findAndCacheLoginClientUuid());
    }
  }
}
