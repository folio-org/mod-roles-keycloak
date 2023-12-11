package org.folio.roles.integration.kafka.model;

import java.util.List;
import lombok.Data;
import org.folio.roles.domain.dto.Endpoint;

@Data
public class FolioResource {

  private Permission permission;

  private List<Endpoint> endpoints;

  /**
   * Sets permission field and returns {@link FolioResource}.
   *
   * @return modified {@link FolioResource} value
   */
  public FolioResource permission(Permission permission) {
    this.permission = permission;
    return this;
  }

  /**
   * Sets endpoints field and returns {@link FolioResource}.
   *
   * @return modified {@link FolioResource} value
   */
  public FolioResource endpoints(List<Endpoint> endpoints) {
    this.endpoints = endpoints;
    return this;
  }
}
