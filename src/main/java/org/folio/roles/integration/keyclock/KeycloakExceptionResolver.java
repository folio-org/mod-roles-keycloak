package org.folio.roles.integration.keyclock;

import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.springframework.stereotype.Component;

@Component
public class KeycloakExceptionResolver {

  public boolean shouldRetry(Throwable exception) {
    if (exception instanceof KeycloakApiException keycloakApiException) {
      var status = keycloakApiException.getStatus();
      return status != null && status.value() >= 500;
    }
    return false;
  }
}
