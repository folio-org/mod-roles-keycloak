package org.folio.roles.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.common.utils.permission.model.PermissionData;

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
   * Permission data mapping overrides.
   */
  @JsonIgnore
  private Map<String, PermissionData> permissionMappingOverrides;

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

  /**
   * Sets permissionMappingOverrides field and returns {@link CapabilityEvent}.
   *
   * @return modified {@link CapabilityEvent} value
   */
  public CapabilityEvent permissionMappingOverrides(Map<String, PermissionData> permissionMappingOverrides) {
    this.permissionMappingOverrides = permissionMappingOverrides;
    return this;
  }
}
