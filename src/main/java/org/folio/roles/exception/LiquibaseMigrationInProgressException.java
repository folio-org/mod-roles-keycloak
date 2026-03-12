package org.folio.roles.exception;

public class LiquibaseMigrationInProgressException extends RuntimeException {

  public LiquibaseMigrationInProgressException(String message) {
    super(message);
  }
}
