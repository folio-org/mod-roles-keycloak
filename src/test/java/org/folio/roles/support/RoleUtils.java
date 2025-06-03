package org.folio.roles.support;

import static org.folio.roles.domain.dto.RoleType.REGULAR;

import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.entity.RoleEntity;
import org.folio.roles.domain.entity.type.EntityRoleType;
import org.keycloak.representations.idm.RoleRepresentation;

@UtilityClass
public class RoleUtils {

  public static final UUID ROLE_ID = UUID.randomUUID();
  public static final String ROLE_NAME = "role1";
  public static final String ROLE_DESCRIPTION = "role1_description";

  public static final UUID ROLE_ID_2 = UUID.randomUUID();
  public static final String ROLE_NAME_2 = "role2";
  public static final String ROLE_DESCRIPTION_2 = "role2_description";

  public static final UUID ROLE_ID_3 = UUID.randomUUID();
  public static final String ROLE_NAME_3 = "role3";
  public static final String ROLE_DESCRIPTION_3 = "role3_description";

  public static Role role() {
    var role = new Role();
    role.setId(ROLE_ID);
    role.setName(ROLE_NAME);
    role.setDescription(ROLE_DESCRIPTION);
    role.setType(REGULAR);
    return role;
  }

  public static Role role2() {
    var role = new Role();
    role.setId(ROLE_ID_2);
    role.setName(ROLE_NAME_2);
    role.setDescription(ROLE_DESCRIPTION_2);
    role.setType(REGULAR);
    return role;
  }

  public static Role defaultRole() {
    var role = new Role();
    role.setId(ROLE_ID_3);
    role.setName(ROLE_NAME_3);
    role.setDescription(ROLE_DESCRIPTION_3);
    role.setType(RoleType.DEFAULT);
    return role;
  }

  public static RoleEntity roleEntity() {
    var roleEntity = new RoleEntity();
    roleEntity.setId(ROLE_ID_2);
    roleEntity.setName(ROLE_NAME_2);
    roleEntity.setDescription(ROLE_DESCRIPTION_2);
    roleEntity.setType(EntityRoleType.REGULAR);
    return roleEntity;
  }

  public static RoleEntity defaultRoleEntity() {
    var roleEntity = new RoleEntity();
    roleEntity.setId(ROLE_ID_3);
    roleEntity.setName(ROLE_NAME_3);
    roleEntity.setDescription(ROLE_DESCRIPTION_3);
    roleEntity.setType(EntityRoleType.DEFAULT);
    return roleEntity;
  }

  public static RoleRepresentation keycloakRole() {
    var roleRepresentation = new RoleRepresentation();
    roleRepresentation.setId(ROLE_ID.toString());
    roleRepresentation.setName(ROLE_NAME);
    roleRepresentation.setDescription(ROLE_DESCRIPTION);
    return roleRepresentation;
  }

  public static RoleRepresentation keycloakRole(Role role) {
    var keycloakRole = new RoleRepresentation();
    keycloakRole.setId(role.getId().toString());
    keycloakRole.setName(role.getName());
    keycloakRole.setDescription(role.getDescription());
    return keycloakRole;
  }
}
