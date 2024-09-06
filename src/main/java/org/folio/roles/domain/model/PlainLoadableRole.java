package org.folio.roles.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlainLoadableRole extends Role {

  @Valid @Size(min = 1)
  @Schema(name = "permissions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Set<String> permissions = new HashSet<>();

  @Override
  public PlainLoadableRole id(UUID id) {
    return (PlainLoadableRole) super.id(id);
  }

  @Override
  public PlainLoadableRole name(String name) {
    return (PlainLoadableRole) super.name(name);
  }

  @Override
  public PlainLoadableRole description(String description) {
    return (PlainLoadableRole) super.description(description);
  }

  public PlainLoadableRole type(RoleType type) {
    this.setType(type);
    return this;
  }

  public PlainLoadableRole permissions(Set<String> permissions) {
    this.permissions = permissions;
    return this;
  }
}
