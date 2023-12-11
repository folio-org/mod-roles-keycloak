package org.folio.roles.integration.keyclock.exception;

import java.io.Serial;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class KeycloakApiException extends RuntimeException {

  @Serial private static final long serialVersionUID = -2562687935297687250L;
  private final HttpStatus status;

  /**
   * Creates a new {@link KeycloakApiException} with message and cause for failed communication with keycloak.
   *
   * @param message - error message as {@link String} object
   * @param cause - error cause as {@link Throwable} object
   */
  public KeycloakApiException(String message, Throwable cause, int status) {
    super(message, cause);
    this.status = HttpStatus.resolve(status);
  }
}
