package org.folio.roles.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.roles.integration.kafka.filter.mmd.ModuleDataProvider;
import org.folio.roles.integration.kafka.filter.mmd.ModulePropertiesModuleDataProvider;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
public class ModuleDataProviderTest {

  private final ModuleDataProvider moduleDataProvider = new ModulePropertiesModuleDataProvider();

  @Test
  void getModuleData_positive() {
    var moduleData = moduleDataProvider.getModuleData();

    assertThat(moduleData).isNotNull();
    assertThat(moduleData.name()).isNotBlank();
    assertThat(moduleData.version()).isNotBlank();
  }
}
