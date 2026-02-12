package org.folio.roles.integration.keyclock;

import java.lang.reflect.Method;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.jspecify.annotations.NonNull;
import org.springframework.resilience.retry.MethodRetryPredicate;

public class KeycloakMethodRetryPredicate implements MethodRetryPredicate {

  @Override
  public boolean shouldRetry(@NonNull Method method, @NonNull Throwable exception) {
    if (exception instanceof KeycloakApiException keycloakApiException) {
      var status = keycloakApiException.getStatus();
      return status != null && status.value() >= 500;
    }
    return false;
  }
}
