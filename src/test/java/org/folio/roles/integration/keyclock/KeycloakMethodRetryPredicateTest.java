package org.folio.roles.integration.keyclock;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.WebApplicationException;
import java.lang.reflect.Method;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakMethodRetryPredicateTest {

  @Nested
  @DisplayName("shouldRetry")
  class ShouldRetry {

    @Test
    void positive_returnsTrueForKeycloakApiExceptionWithStatus500() throws NoSuchMethodException {
      var webApplicationException = new WebApplicationException("Server error", 500);
      var exception = new KeycloakApiException("Server error", webApplicationException);
      var predicate = new KeycloakMethodRetryPredicate();
      Method method = KeycloakRoleService.class.getMethod("findByName", String.class);

      assertThat(predicate.shouldRetry(method, exception)).isTrue();
    }

    @Test
    void positive_returnsFalseForKeycloakApiExceptionWithStatus400() throws NoSuchMethodException {
      var webApplicationException = new WebApplicationException("Client error", 400);
      var exception = new KeycloakApiException("Client error", webApplicationException);
      var predicate = new KeycloakMethodRetryPredicate();
      Method method = KeycloakRoleService.class.getMethod("findByName", String.class);

      assertThat(predicate.shouldRetry(method, exception)).isFalse();
    }

    @Test
    void positive_returnsFalseForGenericException() throws NoSuchMethodException {
      var exception = new RuntimeException("Generic error");
      var predicate = new KeycloakMethodRetryPredicate();
      Method method = KeycloakRoleService.class.getMethod("findByName", String.class);

      assertThat(predicate.shouldRetry(method, exception)).isFalse();
    }
  }
}
