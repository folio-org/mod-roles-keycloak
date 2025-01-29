package org.folio.roles.domain.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CapabilitiesToManageCapabilities {

  /**
   * Capabilities to view capabilities.
   */
  private List<String> viewCapabilities = new ArrayList<>();

  /**
   * Capabilities to edit capabilities.
   */
  private List<String> editCapabilities = new ArrayList<>();
}
