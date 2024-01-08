package org.folio.roles.domain.entity.type;

import org.folio.roles.domain.dto.CapabilityType;

public enum EntityCapabilityType {

  SETTINGS,
  DATA,
  PROCEDURAL;

  /**
   * Creates {@link EntityCapabilityType} from {@link CapabilityType} enum value.
   *
   * @param type - {@link CapabilityType} to process
   * @return {@link EntityCapabilityType} from {@link CapabilityType}
   */
  public static EntityCapabilityType from(CapabilityType type) {
    return EntityCapabilityType.valueOf(type.name());
  }
}
