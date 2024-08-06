package org.folio.roles.exception;

import static org.folio.common.domain.model.error.ErrorCode.VALIDATION_ERROR;

import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import org.folio.common.domain.model.error.ErrorCode;

@Value
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ServiceException extends RuntimeException {

  @Serial private static final long serialVersionUID = 2634857589243742250L;

  String key;
  String value;
  ErrorCode errorCode;

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

  public ServiceException(String message, Throwable cause) {
    super(message, cause);

    this.key = "cause";
    this.value = cause.getMessage();
    this.errorCode = VALIDATION_ERROR;
  }
}
