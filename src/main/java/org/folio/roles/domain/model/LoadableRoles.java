package org.folio.roles.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class LoadableRoles {

  @NotNull @Valid
  private List<@Valid LoadableRole> roles = new ArrayList<>();
}