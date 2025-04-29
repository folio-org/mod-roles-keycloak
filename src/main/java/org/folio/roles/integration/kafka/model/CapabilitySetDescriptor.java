package org.folio.roles.integration.kafka.model;

import java.util.List;
import java.util.Map;
import lombok.Data;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilityType;

@Data
public class CapabilitySetDescriptor {

  /**
   * Generated capability set name.
   */
  private String name;

  /**
   * Capability set description.
   */
  private String description;

  /**
   * Capability set resource name.
   */
  private String resource;

  /**
   * A Folio permission name.
   */
  private String permission;

  /**
   * Capability set action.
   */
  private CapabilityAction action;

  /**
   * Capability set descriptor module identifier, nullable.
   */
  private String moduleId;

  /**
   * Capability set descriptor application identifier, nullable.
   */
  private String applicationId;

  /**
   * Capability set type.
   */
  private CapabilityType type;

  /**
   * Child capability name-actions map.
   */
  private Map<String, List<CapabilityAction>> capabilities;

  /**
   * Visible in UI.
   */
  private boolean visible;

  /**
   * Sets resource field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor resource(String resource) {
    this.resource = resource;
    return this;
  }

  /**
   * Sets name field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets description field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Sets type field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor type(CapabilityType type) {
    this.type = type;
    return this;
  }

  /**
   * Sets action field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor action(CapabilityAction action) {
    this.action = action;
    return this;
  }

  /**
   * Sets applicationId field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor applicationId(String applicationId) {
    this.applicationId = applicationId;
    return this;
  }

  /**
   * Sets moduleId for {@link CapabilitySetDescriptor} and returns {@link CapabilitySetDescriptor}.
   *
   * @return this {@link CapabilitySetDescriptor} with new moduleId value
   */
  public CapabilitySetDescriptor moduleId(String moduleId) {
    this.moduleId = moduleId;
    return this;
  }

  /**
   * Sets capabilities field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor capabilities(
    Map<String, List<CapabilityAction>> capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  /**
   * Sets permission field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor permission(String permission) {
    this.permission = permission;
    return this;
  }

  /**
   * Sets visible field and returns {@link CapabilitySetDescriptor}.
   *
   * @return modified {@link CapabilitySetDescriptor} value
   */
  public CapabilitySetDescriptor visible(Boolean visible) {
    this.visible = visible != null && visible;
    return this;
  }
}
