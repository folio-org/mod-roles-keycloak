package org.folio.roles.integration.kafka.filter.mmd;

@FunctionalInterface
public interface ModuleDataProvider {

  ModuleData getModuleData();
}
