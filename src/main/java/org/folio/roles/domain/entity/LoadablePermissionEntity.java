package org.folio.roles.domain.entity;

import static org.springframework.data.domain.Sort.Direction.ASC;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.folio.roles.domain.entity.key.LoadablePermissionKey;
import org.springframework.data.domain.Sort;

@Data
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

  /**
   * Fetched eagerly on purpose: a lazy {@code @ManyToOne} makes Hibernate materialise a {@code HibernateProxy}
   * whenever this entity is <em>loaded</em> (not merely navigated), which fails in the GraalVM native image because
   * the bytecode provider is {@code none} (see the native build args in the pom — ByteBuddy cannot define classes at
   * runtime). This is the only to-one association in the model; the role is read-only here (mapped to the same
   * column as {@link #roleId}) and is shared by all permissions of a role, so the persistence context de-duplicates
   * the fetch.
   */
  @ManyToOne(fetch = FetchType.EAGER)
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
}
