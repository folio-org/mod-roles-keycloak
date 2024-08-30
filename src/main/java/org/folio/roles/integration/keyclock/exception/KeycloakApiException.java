package org.folio.roles.integration.keyclock.exception;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.Serial;
import java.util.Optional;
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

  /**
   * Creates a new {@link KeycloakApiException} with message and cause for failed communication with keycloak.
   *
   * @param message - error message as {@link String} object
   * @param cause - error cause as {@link Throwable} object
   */
  public KeycloakApiException(String message, WebApplicationException cause) {
    super(message, cause);
    this.status = getResponseStatus(cause);
  }

  /**
   * Returns exception http status or {@link HttpStatus#INTERNAL_SERVER_ERROR} value.
   *
   * @param exception - exception to process
   * @return {@link WebApplicationException} error http status
   */
  private static HttpStatus getResponseStatus(WebApplicationException exception) {
    return Optional.ofNullable(exception)
      .map(WebApplicationException::getResponse)
      .map(Response::getStatus)
      .map(HttpStatus::resolve)
      .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
