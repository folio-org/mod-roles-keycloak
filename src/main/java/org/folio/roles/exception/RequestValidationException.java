package org.folio.roles.exception;

import static org.folio.common.domain.model.error.ErrorCode.VALIDATION_ERROR;

import java.io.Serial;
import lombok.Getter;
import org.folio.common.domain.model.error.ErrorCode;

@Getter
public class RequestValidationException extends RuntimeException {

  @Serial private static final long serialVersionUID = -97083844736541490L;

  private final String key;
  private final String value;
  private final ErrorCode errorCode;

  /**
   * Creates {@link RequestValidationException} object for given message, key and value.
   *
   * @param message - validation error message
   */
  public RequestValidationException(String message) {
    super(message);

    this.key = null;
    this.value = null;
    this.errorCode = VALIDATION_ERROR;
  }

  /**
   * Creates {@link RequestValidationException} object for given message, key and value.
   *
   * @param message - validation error message
   * @param key - validation key as field or parameter name
   * @param value - invalid parameter value
   */
  public RequestValidationException(String message, String key, Object value) {
    super(message);

    this.key = key;
    this.value = value != null ? value.toString() : null;
    this.errorCode = VALIDATION_ERROR;
  }
}
