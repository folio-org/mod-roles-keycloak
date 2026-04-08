package org.folio.roles.integration.kafka.filter;

import java.util.Objects;

public final class TenantsNotEnabledException extends RuntimeException {

  private static final String DEFAULT_MESSAGE_FORMAT = "No tenants are enabled for the module: moduleId = %s";

  private TenantsNotEnabledException(String message) {
    super(message);
  }

  public static TenantsNotEnabledException forModule(String moduleId) {
    Objects.requireNonNull(moduleId, "Module ID must not be null");
    return new TenantsNotEnabledException(String.format(DEFAULT_MESSAGE_FORMAT, moduleId));
  }
}
