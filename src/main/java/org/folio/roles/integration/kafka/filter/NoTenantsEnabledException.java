package org.folio.roles.integration.kafka.filter;

import java.util.Objects;

public class NoTenantsEnabledException extends RuntimeException {

  private static final String DEFAULT_MESSAGE_FORMAT = "No tenants are enabled for the module: moduleId = %s";

  public static NoTenantsEnabledException forModule(String moduleId) {
    Objects.requireNonNull(moduleId, "Module ID must not be null");
    return new NoTenantsEnabledException(String.format(DEFAULT_MESSAGE_FORMAT, moduleId));
  }

  private NoTenantsEnabledException(String message) {
    super(message);
  }
}
