package org.folio.roles.integration.kafka.filter.mmd;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.List;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ChainedModuleDataProvider implements ModuleDataProvider {

  private final List<ModuleDataProvider> providers;

  public ChainedModuleDataProvider(List<ModuleDataProvider> providers) {
    if (isEmpty(providers)) {
      throw new IllegalArgumentException("At least one module data provider must be set");
    }
    this.providers = providers;
  }

  @Override
  public ModuleData getModuleData() {
    for (ModuleDataProvider provider : providers) {
      try {
        return provider.getModuleData();
      } catch (IllegalStateException e) {
        log.info("ModuleDataProvider {} failed to provide module data with error: {}",
          provider.getClass().getSimpleName(), e.getMessage());
      }
    }
    throw new IllegalStateException("No module data can be retrieved. Check configured module data providers");
  }
}
