package org.folio.roles.integration.keyclock;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.WebApplicationException;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakExceptionResolverTest {

  @Nested
  @DisplayName("shouldRetry")
  class ShouldRetry {

    @Test
    void positive_shouldRetryReturnsTrueForKeycloakApiExceptionWithStatus500() {
      var webApplicationException = new WebApplicationException("Server error", 500);
      var exception = new KeycloakApiException("Server error", webApplicationException);
      var resolver = new KeycloakExceptionResolver();

      assertThat(resolver.shouldRetry(exception)).isTrue();
    }

    @Test
    void positive_shouldRetryReturnsFalseForKeycloakApiExceptionWithStatus400() {
      var webApplicationException = new WebApplicationException("Client error", 400);
      var exception = new KeycloakApiException("Client error", webApplicationException);
      var resolver = new KeycloakExceptionResolver();

      assertThat(resolver.shouldRetry(exception)).isFalse();
    }

    @Test
    void positive_shouldRetryReturnsFalseForWebApplicationException() {
      var exception = new WebApplicationException("Client error", 400);
      var resolver = new KeycloakExceptionResolver();

      assertThat(resolver.shouldRetry(exception)).isFalse();
    }
  }
}
