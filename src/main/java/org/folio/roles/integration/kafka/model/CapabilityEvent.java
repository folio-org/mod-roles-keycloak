package org.folio.roles.integration.kafka.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class CapabilityEvent {

  /**
   * Module identifier.
   */
  private String moduleId;

  /**
   * Module type: be or ui.
   */
  private ModuleType moduleType;

  /**
   * Application identifier.
   */
  private String applicationId;

  /**
   * List with folio resources (permission and corresponding endpoints).
   */
  private List<FolioResource> resources;

  /**
   * Sets moduleId field and returns {@link CapabilityEvent}.
   *
   * @return modified {@link CapabilityEvent} value
   */
  public CapabilityEvent moduleId(String moduleId) {
    this.moduleId = moduleId;
    return this;
  }

  /**
   * Sets moduleType field and returns {@link CapabilityEvent}.
   *
   * @return modified {@link CapabilityEvent} value
   */
  public CapabilityEvent moduleType(ModuleType moduleType) {
    this.moduleType = moduleType;
    return this;
  }

  /**
   * Sets applicationId field and returns {@link CapabilityEvent}.
   *
   * @return modified {@link CapabilityEvent} value
   */
  public CapabilityEvent applicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /**
   * Sets resources field and returns {@link CapabilityEvent}.
   *
   * @return modified {@link CapabilityEvent} value
   */
  public CapabilityEvent resources(List<FolioResource> resources) {
    this.resources = resources;
    return this;
  }
}
