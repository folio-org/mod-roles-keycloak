package org.folio.roles.configuration.property;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.folio-permissions.mapping")
public class PermissionMappingProperties {

  @NotNull(message = "Source path must be set")
  private String sourcePath;
}
