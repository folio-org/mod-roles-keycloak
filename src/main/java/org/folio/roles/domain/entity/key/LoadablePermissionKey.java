package org.folio.roles.domain.entity.key;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class LoadablePermissionKey implements Comparable<LoadablePermissionKey>, Serializable {

  @Serial private static final long serialVersionUID = 5181693955891076504L;
  private static final Comparator<LoadablePermissionKey> KEY_COMPARATOR = getComparator();

  private UUID roleId;

  private String permissionName;

  @Override
  public int compareTo(LoadablePermissionKey other) {
    return KEY_COMPARATOR.compare(this, other);
  }

  private static Comparator<LoadablePermissionKey> getComparator() {
    return nullsFirst(comparing(LoadablePermissionKey::getRoleId))
      .thenComparing(nullsFirst(comparing(LoadablePermissionKey::getPermissionName)));
  }
}
