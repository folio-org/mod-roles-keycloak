package org.folio.roles.integration.keyclock.model.policy;

import static java.util.Objects.nonNull;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserPolicy extends BasePolicy {

  private List<UUID> users;
  private Config config;

  public boolean hasConfigValue() {
    return nonNull(config) && isNotBlank(config.getUsers());
  }

  @Data
  public static class Config {

    private String users;
  }
}
