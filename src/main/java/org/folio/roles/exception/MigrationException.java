package org.folio.roles.exception;

import java.io.Serial;

public class MigrationException extends RuntimeException {

  @Serial private static final long serialVersionUID = -2911230296294807828L;

  public MigrationException(String message) {
    super(message);
  }

  public MigrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
