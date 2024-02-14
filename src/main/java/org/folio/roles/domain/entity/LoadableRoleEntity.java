package org.folio.roles.domain.entity;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.folio.roles.domain.entity.type.EntityLoadableRoleType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@Data
@Entity
@Table(name = "role_loadable")
@EqualsAndHashCode(callSuper = true)
public class LoadableRoleEntity extends RoleEntity {

  public static final Sort DEFAULT_LOADABLE_ROLE_SORT = Sort.by(Direction.ASC, "name");

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "type", columnDefinition = "role_loadable_type")
  private EntityLoadableRoleType type;

  @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    mappedBy = "role",
    orphanRemoval = true)
  private Set<LoadablePermissionEntity> permissions = new HashSet<>();

  public void setPermissions(Collection<LoadablePermissionEntity> newPermissions) {
    removeExistingPermissions();

    emptyIfNull(newPermissions).forEach(this::addPermission);
  }

  public void addPermission(LoadablePermissionEntity permission) {
    permission.setRoleId(this.getId());
    permission.setRole(this);

    permissions.add(permission);
  }

  private void removeExistingPermissions() {
    for (var itr = permissions.iterator(); itr.hasNext(); ) {
      var perm = itr.next();

      perm.setRoleId(null);
      perm.setRole(null);
      itr.remove();
    }
  }
}
