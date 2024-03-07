package org.folio.roles.domain.entity.type;

import org.folio.roles.domain.dto.LoadableRoleType;

public enum EntityLoadableRoleType {

  DEFAULT,
  SUPPORT;

  /**
   * Creates {@link EntityLoadableRoleType} from {@link LoadableRoleType} enum value.
   *
   * @param type - {@link LoadableRoleType} to process
   * @return {@link EntityLoadableRoleType} from {@link LoadableRoleType}
   */
  public static EntityLoadableRoleType from(LoadableRoleType type) {
    return EntityLoadableRoleType.valueOf(type.name());
  }
}
