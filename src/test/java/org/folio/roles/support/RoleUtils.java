package org.folio.roles.support;

import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.integration.keyclock.model.KeycloakRole;

@UtilityClass
public class RoleUtils {

  public static final UUID ROLE_ID = UUID.fromString("1e985e76-e9ca-401c-ad8e-0d121a11111e");
  public static final String ROLE_NAME = "role1";
  public static final String ROLE_DESCRIPTION = "role1_description";
  public static final SourceType ROLE_SOURCE = SourceType.SYSTEM;

  public static final UUID ROLE_ID_2 = UUID.fromString("2e985e76-e9ca-401c-ad8e-0d121a22222e");
  public static final String ROLE_NAME_2 = "role2";
  public static final String ROLE_DESCRIPTION_2 = "role2_description";
  public static final SourceType ROLE_SOURCE_2 = SourceType.USER;

  public static Role role() {
    var role = new Role();
    role.setId(ROLE_ID);
    role.setName(ROLE_NAME);
    role.setDescription(ROLE_DESCRIPTION);
    role.setSource(ROLE_SOURCE);
    return role;
  }

  public static Role role2() {
    var role = new Role();
    role.setId(ROLE_ID_2);
    role.setName(ROLE_NAME_2);
    role.setDescription(ROLE_DESCRIPTION_2);
    role.setSource(ROLE_SOURCE_2);
    return role;
  }

  public static KeycloakRole keycloakRole() {
    var keycloakRole = new KeycloakRole();
    keycloakRole.setId(ROLE_ID);
    keycloakRole.setName(ROLE_NAME);
    keycloakRole.setDescription(ROLE_DESCRIPTION);
    return keycloakRole;
  }

  public static KeycloakRole keycloakRole(Role role) {
    var keycloakRole = new KeycloakRole();
    keycloakRole.setId(role.getId());
    keycloakRole.setName(role.getName());
    keycloakRole.setDescription(role.getDescription());
    return keycloakRole;
  }
}
