package org.folio.roles.exception;

public class UnassignedPermissionsException extends RuntimeException {

  public UnassignedPermissionsException(String errorMessage) {
    super(errorMessage);
  }
}
