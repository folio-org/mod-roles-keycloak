package org.folio.roles.configuration;

import org.folio.roles.integration.permissions.PermissionsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientConfig {

  @Bean
  public PermissionsClient permissionsClient(HttpServiceProxyFactory factory) {
    return factory.createClient(PermissionsClient.class);
  }
}
