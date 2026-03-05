package org.folio.roles.integration.keyclock.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakConfigurationPropertiesTest {

  @Test
  void permissionsDefaults() {
    var props = new KeycloakConfigurationProperties();

    assertThat(props.getPermissions().getParallelism()).isEqualTo(4);
    assertThat(props.getPermissions().getBatchSize()).isEqualTo(50);
  }
}
