package org.folio.roles.support;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.entity.PermissionEntity;
import org.folio.roles.integration.kafka.model.Permission;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthResourceUtils {

  public static Permission permission(String name, String... subPermissions) {
    return new Permission().permissionName(name).subPermissions(List.of(subPermissions));
  }

  public static Permission permission(UUID id, String name, String... subPermissions) {
    return new Permission().id(id).permissionName(name).subPermissions(List.of(subPermissions));
  }

  public static PermissionEntity permissionEntity(UUID id, String name, String... subPermissions) {
    var permissionEntity = new PermissionEntity();
    permissionEntity.setId(id);
    permissionEntity.setPermissionName(name);
    permissionEntity.setSubPermissions(List.of(subPermissions));
    return permissionEntity;
  }

  public static Permission fooPermission(UUID id) {
    return new Permission()
      .id(id)
      .permissionName("foo.entities.collection.get")
      .displayName("Foo - get entities collection by query")
      .description("Retrieve foo entities by query")
      .subPermissions(List.of("foo.entities.item.get"))
      .visible(false);
  }

  public static PermissionEntity fooPermissionEntity(UUID id) {
    var entity = new PermissionEntity();
    entity.setId(id);
    entity.setPermissionName("foo.entities.collection.get");
    entity.setDisplayName("Foo - get entities collection by query");
    entity.setDescription("Retrieve foo entities by query");
    entity.setSubPermissions(List.of("foo.entities.item.get"));
    entity.setVisible(false);
    return entity;
  }
}
