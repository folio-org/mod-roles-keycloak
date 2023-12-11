package org.folio.roles.exception;

import static org.folio.common.domain.model.error.ErrorCode.VALIDATION_ERROR;

import java.io.Serial;
import lombok.Getter;
import org.folio.common.domain.model.error.ErrorCode;

@Getter
public class ServiceException extends RuntimeException {

  @Serial private static final long serialVersionUID = 2634857589243742250L;

  private final String key;
  private final String value;
  private final ErrorCode errorCode;

  /**
   * Creates {@link ServiceException} object for given message, key
   * and value.
   *
   * @param message - validation error message
   * @param key - validation key as field or parameter name
   * @param value - invalid parameter value
   */
  public ServiceException(String message, String key, String value) {
    super(message);

    this.key = key;
    this.value = value;
    this.errorCode = VALIDATION_ERROR;
  }
}
