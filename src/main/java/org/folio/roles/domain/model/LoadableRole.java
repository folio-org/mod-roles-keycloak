package org.folio.roles.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Metadata;
import org.folio.roles.domain.dto.Role;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LoadableRole extends Role {

  @NotNull
  @Valid
  private LoadableRoleType type;

  @Valid
  @Size(min = 1)
  private Set<LoadablePermission> permissions;

  @Builder
  private LoadableRole(UUID id, String name, String description, LoadableRoleType type,
    Set<LoadablePermission> permissions, Metadata metadata) {
    setId(id);
    setName(name);
    setDescription(description);
    setMetadata(metadata);

    this.type = type;
    this.permissions = permissions;
  }
}

