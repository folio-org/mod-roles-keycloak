package org.folio.roles.domain.entity.type;

import org.folio.roles.domain.dto.RoleType;

public enum EntityRoleType {

  DEFAULT,
  REGULAR,
  CONSORTIUM;

  /**
   * Creates {@link EntityRoleType} from {@link RoleType} enum value.
   *
   * @param type - {@link RoleType} to process
   * @return {@link EntityRoleType} from {@link RoleType}
   */
  public static EntityRoleType from(RoleType type) {
    return EntityRoleType.valueOf(type.name());
  }
}
