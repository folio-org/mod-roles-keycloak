package org.folio.roles.domain.entity;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

@Data
@Entity
@Table(name = "role_loadable")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LoadableRoleEntity extends RoleEntity {

  public static final Sort DEFAULT_LOADABLE_ROLE_SORT = Sort.by(Direction.ASC, "name");

  @OneToMany(cascade = CascadeType.ALL,
    fetch = FetchType.LAZY,
    mappedBy = "role",
    orphanRemoval = true)
  private Set<LoadablePermissionEntity> permissions = new HashSet<>();

  @Schema(name = "loaded_from_file")
  private boolean loadedFromFile = false;

  public void setPermissions(Collection<LoadablePermissionEntity> newPermissions) {
    removeExistingPermissions();

    emptyIfNull(newPermissions).forEach(this::addPermission);
  }

  public void addPermission(LoadablePermissionEntity permission) {
    permission.setRoleId(this.getId());
    permission.setRole(this);

    permissions.add(permission);
  }

  public void removePermission(LoadablePermissionEntity permission) {
    permission.setRole(null);

    permissions.remove(permission);
  }

  private void removeExistingPermissions() {
    for (var itr = permissions.iterator(); itr.hasNext(); ) {
      var perm = itr.next();

      perm.setRole(null);
      itr.remove();
    }
  }

  public boolean equalsLogically(LoadableRoleEntity other) {
    if (this == other) {
      return true;
    }

    return Objects.equals(getId(), other.getId())
      && Objects.equals(getName(), other.getName())
      && Objects.equals(getDescription(), other.getDescription())
      && Objects.equals(getType(), other.getType());
  }
}
