package org.folio.roles.integration.keyclock.model.policy;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.logging.log4j.util.Strings.isNotBlank;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RolePolicy extends BasePolicy {

  private List<Role> roles;
  private Config config;

  public boolean hasRoles() {
    return isNotEmpty(roles);
  }

  public boolean hasConfigValue() {
    return nonNull(config) && isNotBlank(config.getRoles());
  }

  @Data
  public static class Config {

    private String roles;
  }
}

