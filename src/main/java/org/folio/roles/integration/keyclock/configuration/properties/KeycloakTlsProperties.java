package org.folio.roles.integration.keyclock.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class KeycloakTlsProperties {

  private boolean enabled;
  private String trustStorePath;
  private String trustStorePassword;
}
