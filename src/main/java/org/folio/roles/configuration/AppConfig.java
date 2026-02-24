package org.folio.roles.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableCaching
@EnableFeignClients("org.folio.roles.integration")
@Configuration
public class AppConfig {
}
