package org.folio.roles.integration.kafka.filter.mmd;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class AppPropertiesModuleDataProvider implements ModuleDataProvider {

  private final String applicationName;
  private final String applicationVersion;

  public AppPropertiesModuleDataProvider(String applicationName, String applicationVersion) {
    this.applicationName = applicationName;
    this.applicationVersion = applicationVersion;
  }

  @Override
  public ModuleData getModuleData() {
    if (isBlank(applicationName) || isBlank(applicationVersion)) {
      throw new IllegalStateException("Application name or version is blank. Cannot provide module data.");
    } else {
      return new ModuleData(applicationName, applicationVersion);
    }
  }
}
