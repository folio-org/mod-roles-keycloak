package org.folio.roles.integration.mte.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.roles.integration.mte.TenantEntitlementsClient;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@UnitTest
class MteConfigurationTest {

  @Test
  void tenantEntitlementsClient_positive_returnsProxy() {
    var props = new MteConfigurationProperties();
    props.setUrl("http://localhost:8080");

    var config = new MteConfiguration(props);
    var client = config.tenantEntitlementsClient(RestClient.builder());

    assertThat(client).isNotNull().isInstanceOf(TenantEntitlementsClient.class);
  }
}
