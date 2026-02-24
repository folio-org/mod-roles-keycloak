package org.folio.roles.support;

import static java.lang.String.format;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.AuditableUtils.populateAuditable;
import static org.instancio.Instancio.gen;
import static org.instancio.Select.field;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.LoadablePermission;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.instancio.Instancio;
import org.instancio.Model;

@UtilityClass
public class LoadablePermissionUtils {

  private static final Model<LoadablePermission> LOADABLE_PERMISSION_MODEL = Instancio.of(LoadablePermission.class)
    .supply(field(LoadablePermission::getPermissionName), generatePermName())
    .toModel();

  public static LoadablePermission loadablePermission() {
    return Instancio.of(LOADABLE_PERMISSION_MODEL).create();
  }

  public static LoadablePermission loadablePermission(UUID roleId) {
    return Instancio.of(LOADABLE_PERMISSION_MODEL)
      .set(field(LoadablePermission::getRoleId), roleId)
      .create();
  }

  public static LoadablePermission loadablePermission(UUID roleId, String permissionName) {
    return Instancio.of(LOADABLE_PERMISSION_MODEL)
      .set(field(LoadablePermission::getRoleId), roleId)
      .set(field(LoadablePermission::getPermissionName), permissionName)
      .create();
  }

  public static List<LoadablePermission> loadablePermissions(int maxSize) {
    return loadablePermissions(1, maxSize);
  }

  public static List<LoadablePermission> loadablePermissions(int minSize, int maxSize) {
    return Instancio.ofList(LOADABLE_PERMISSION_MODEL)
      .size(gen().ints().range(minSize, maxSize).get())
      .create();
  }

  public static List<LoadablePermissionEntity> loadablePermissionEntities(List<LoadablePermission> perms) {
    return mapItems(perms, perm -> loadablePermissionEntity(perm.getRoleId(), perm));
  }

  public static LoadablePermissionEntity loadablePermissionEntity(UUID roleId, LoadablePermission perm) {
    var entity = new LoadablePermissionEntity();

    entity.setRoleId(roleId);
    entity.setPermissionName(perm.getPermissionName());
    entity.setCapabilityId(perm.getCapabilityId());
    entity.setCapabilitySetId(perm.getCapabilitySetId());
    populateAuditable(entity, perm.getMetadata());

    return entity;
  }

  public static LoadablePermissionEntity loadablePermissionEntity(UUID roleId, UUID capabilityId,
    UUID capabilitySetId) {
    return loadablePermissionEntity(roleId,
      loadablePermission().capabilityId(capabilityId).capabilitySetId(capabilitySetId));
  }

  public static LoadableRoleEntity loadableRoleEntity() {
    var roleId = UUID.randomUUID();
    var loadableRole = new LoadableRoleEntity();
    loadableRole.setId(roleId);
    loadableRole.setName("roleName");
    loadableRole.setDescription("roleDescription");
    return loadableRole;
  }

  private static Supplier<String> generatePermName() {
    return () -> format("permission.resource.%s.%s.%s",
      gen().ints().min(0).get(),
      gen().oneOf("collection", "item").get(),
      gen().oneOf("get", "post", "put", "delete").get());
  }
}
