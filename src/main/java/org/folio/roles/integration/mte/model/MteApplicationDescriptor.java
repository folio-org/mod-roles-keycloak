package org.folio.roles.integration.mte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single application descriptor returned by mgr-tenant-entitlements.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MteApplicationDescriptor {

  /**
   * The versioned application identifier (e.g., {@code my-app-1.0.0}).
   */
  private String id;
}
