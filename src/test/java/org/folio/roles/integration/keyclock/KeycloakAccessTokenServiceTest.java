package org.folio.roles.integration.keyclock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.folio.roles.integration.keyclock.client.AccessTokenClient;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.integration.keyclock.model.KeycloakRealmConfiguration;
import org.folio.roles.integration.keyclock.model.TokenRequest;
import org.folio.roles.integration.keyclock.model.TokenResponse;
import org.folio.test.types.UnitTest;
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
class KeycloakAccessTokenServiceTest {

  private static final String CLIENT_ID = "cli-admin";
  private static final String CLIENT_SECRET = "password";
  private static final String GRANT_TYPE = "client_credentials";

  @Mock private AccessTokenClient tokenClient;
  @Mock private KeycloakConfigurationProperties properties;
  @Mock private RealmConfigurationProvider realmConfigurationProvider;

  @InjectMocks private KeycloakAccessTokenService tokenService;

  @BeforeEach
  void beforeEach() {
    when(properties.getGrantType()).thenReturn(GRANT_TYPE);
    when(realmConfigurationProvider.getRealmConfiguration()).thenReturn(kcRealmConfiguration());
  }

  private static KeycloakRealmConfiguration kcRealmConfiguration() {
    return new KeycloakRealmConfiguration().clientId(CLIENT_ID).clientSecret(CLIENT_SECRET);
  }

  @Nested
  @DisplayName("extractAuthHeader")
  class ExtractAuthHeader {

    @Test
    void positive_returns_grant_type_plus_token() {
      var tokenRequest = TokenRequest.builder()
        .grantType(GRANT_TYPE)
        .clientId(CLIENT_ID)
        .clientSecret(CLIENT_SECRET)
        .build();
      var accessToken = "access_token";
      var tokenType = "token_type";
      var tokenResponse = new TokenResponse();
      tokenResponse.setAccessToken(accessToken);
      tokenResponse.setTokenType(tokenType);

      when(tokenClient.retrieveAccessToken(tokenRequest)).thenReturn(tokenResponse);

      var authHeaderValue = tokenService.getToken();

      assertEquals(authHeaderValue, tokenType + " " + accessToken);
    }
  }
}
