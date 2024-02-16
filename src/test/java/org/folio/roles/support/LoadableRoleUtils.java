package org.folio.roles.support;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.AuditableUtils.populateAuditable;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.instancio.Select.all;

import lombok.experimental.UtilityClass;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityLoadableRoleType;
import org.folio.roles.domain.model.LoadableRole;
import org.instancio.Instancio;

@UtilityClass
public class LoadableRoleUtils {

  public static LoadableRole loadableRole() {
    return Instancio.of(LoadableRole.class)
      .supply(all(String.class), gen -> capitalize(randomAlphabetic(10, 20)))
      .create();
  }

  public static LoadableRoleEntity loadableRoleEntity(LoadableRole role) {
    var entity = new LoadableRoleEntity();

    entity.setId(role.getId());
    entity.setName(role.getName());
    entity.setDescription(role.getDescription());
    entity.setType(EntityLoadableRoleType.from(role.getType()));

    entity.setPermissions(mapItems(role.getPermissions(), perm -> loadablePermissionEntity(role.getId(), perm)));

    populateAuditable(entity, role.getMetadata());

    return entity;
  }
}
