package org.folio.roles.domain.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.model.LoadableRoleType.Values;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PlainLoadableRole extends Role {

  @NotNull
  @Valid
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED, defaultValue = Values.DEFAULT_VALUE)
  private LoadableRoleType type;

  @Valid @Size(min = 1)
  @Schema(name = "permissions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private Set<String> permissions;

  public PlainLoadableRole type(LoadableRoleType type) {
    this.type = type;
    return this;
  }
}
