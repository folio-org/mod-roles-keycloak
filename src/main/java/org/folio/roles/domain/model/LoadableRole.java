package org.folio.roles.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Metadata;
import org.folio.roles.domain.dto.Role;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class LoadableRole extends Role {

  @NotNull
  @Valid
  private LoadableRoleType type;

  @Valid
  @Size(min = 1)
  private Set<LoadablePermission> permissions = new HashSet<>();

  public LoadableRole id(UUID id) {
    return (LoadableRole) super.id(id);
  }

  public LoadableRole name(String name) {
    return (LoadableRole) super.name(name);
  }

  public LoadableRole description(String description) {
    return (LoadableRole) super.description(description);
  }

  public LoadableRole type(LoadableRoleType type) {
    this.type = type;
    return this;
  }

  public LoadableRole permissions(Set<LoadablePermission> permissions) {
    this.permissions = permissions;
    return this;
  }

  public LoadableRole metadata(Metadata metadata) {
    return (LoadableRole) super.metadata(metadata);
  }
}

