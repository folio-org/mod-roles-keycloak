package org.folio.roles.domain.entity;

import static org.springframework.data.domain.Sort.Direction.ASC;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.springframework.data.domain.Sort;

@Data
@Log4j2
@Entity
@Table(name = "role_loadable_permission")
@IdClass(LoadablePermissionKey.class)
@EqualsAndHashCode(callSuper = true)
public class LoadablePermissionEntity extends Auditable implements Identifiable<LoadablePermissionKey> {

  public static final Sort DEFAULT_LOADABLE_PERMISSION_SORT = Sort.by(ASC, "roleId", "permissionName");

  @Id
  @Column(name = "role_loadable_id")
  private UUID roleId;

  @Id
  @Column(name = "folio_permission")
  private String permissionName;

  @Column(name = "capability_id")
  private UUID capabilityId;

  @Column(name = "capability_set_id")
  private UUID capabilitySetId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "role_loadable_id", updatable = false, insertable = false)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private LoadableRoleEntity role;

  @Override
  public LoadablePermissionKey getId() {
    return LoadablePermissionKey.of(roleId, permissionName);
  }

  @Override
  public void setId(LoadablePermissionKey id) {
    if (id == null) {
      roleId = null;
      permissionName = null;
    } else {
      roleId = id.getRoleId();
      permissionName = id.getPermissionName();
    }
  }

  @PostPersist
  void logCreated() {
    log.info(
      "Saved role_loadable_permission record: roleId={}, permissionName={}, capabilityId={}, capabilitySetId={}",
      roleId, permissionName, capabilityId, capabilitySetId, persistenceTrace("save"));
  }

  @PostUpdate
  void logUpdated() {
    log.info(
      "Updated role_loadable_permission record: roleId={}, permissionName={}, capabilityId={}, capabilitySetId={}",
      roleId, permissionName, capabilityId, capabilitySetId, persistenceTrace("update"));
  }

  private IllegalStateException persistenceTrace(String operation) {
    return new IllegalStateException("role_loadable_permission " + operation + " stack trace");
  }
}
