package org.folio.roles.integration.keyclock.model;

import java.util.UUID;
import lombok.Data;

@Data
public class KeycloakRole {

  private UUID id;
  private String name;
  private String description;
  private Boolean composite;
  private Boolean clientRole;
  private String containerId;
}
