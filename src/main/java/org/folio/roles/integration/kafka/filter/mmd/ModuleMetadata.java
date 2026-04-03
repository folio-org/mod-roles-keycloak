package org.folio.roles.integration.kafka.filter.mmd;

public interface ModuleMetadata {

  String getModuleName();

  String getModuleVersion();

  default String getModuleId() {
    return getModuleName() + "-" + getModuleVersion();
  }
}
