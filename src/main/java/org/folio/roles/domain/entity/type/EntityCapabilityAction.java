package org.folio.roles.domain.entity.type;

import org.folio.roles.domain.dto.CapabilityAction;

public enum EntityCapabilityAction {
  
  VIEW,
  CREATE,
  EDIT,
  DELETE,
  MANAGE,
  EXECUTE;

  /**
   * Creates {@link EntityCapabilityAction} from {@link CapabilityAction} enum value.
   *
   * @param action - {@link CapabilityAction} to process
   * @return {@link EntityCapabilityAction} from {@link CapabilityAction}
   */
  public static EntityCapabilityAction from(CapabilityAction action) {
    return EntityCapabilityAction.valueOf(action.name());
  }
}
