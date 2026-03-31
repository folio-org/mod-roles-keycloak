package org.folio.roles.integration.mte.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response wrapper for the mgr-tenant-entitlements applications endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MteApplicationDescriptors {

  /**
   * List of application descriptors for this tenant's entitled applications.
   */
  private List<MteApplicationDescriptor> applicationDescriptors;

  /**
   * Total number of records available.
   */
  private Integer totalRecords;
}
