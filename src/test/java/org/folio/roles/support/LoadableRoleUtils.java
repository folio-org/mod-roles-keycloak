package org.folio.roles.support;

import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.AuditableUtils.populateAuditable;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissions;
import static org.folio.roles.support.TestUtils.copy;
import static org.instancio.Select.field;
import static org.instancio.Select.root;

import java.util.ArrayList;
import java.util.Arrays;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.LoadableRole;
import org.folio.roles.domain.dto.LoadableRoles;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityLoadableRoleType;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.OnCompleteCallback;

@UtilityClass
public class LoadableRoleUtils {

  private static final Model<LoadableRole> LOADABLE_ROLE_MODEL = Instancio.of(LoadableRole.class)
    .supply(field(LoadableRole::getPermissions), () -> new ArrayList<>(loadablePermissions(10)))
    .onComplete(root(), completeRoleInit())
    .toModel();

  public static LoadableRole loadableRole() {
    return Instancio.of(LOADABLE_ROLE_MODEL).create();
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

  public static Role regularRole(LoadableRole role) {
    return new Role().id(role.getId())
      .name(role.getName())
      .description(role.getDescription())
      .metadata(copy(role.getMetadata()));
  }

  public static LoadableRoles loadableRoles(LoadableRole... roles) {
    return loadableRoles(roles.length, roles);
  }

  public static LoadableRoles loadableRoles(long totalRecords, LoadableRole... roles) {
    return new LoadableRoles()
      .loadableRoles(Arrays.asList(roles))
      .totalRecords(totalRecords);
  }

  private static OnCompleteCallback<LoadableRole> completeRoleInit() {
    return (LoadableRole role) -> {
      var roleId = role.getId();

      role.setName("Role " + roleId);
      role.setDescription("Description of role " + roleId);

      role.getPermissions().forEach(loadablePermission -> loadablePermission.setRoleId(roleId));
    };
  }
}
