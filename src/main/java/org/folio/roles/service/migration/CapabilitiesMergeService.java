package org.folio.roles.service.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that orchestrates capability migrations and merges. This service provides a clean API for managing capability
 * updates and consolidations.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class CapabilitiesMergeService {

  private final CapabilityDuplicateMigrationService capabilityDuplicateMigrationService;

  /**
   * Merges duplicate capabilities after tenant initialization. This method contains hardcoded migration definitions for
   * known capability duplicates.
   */
  @Transactional
  public void mergeDuplicateCapabilities() {
    log.info("Starting capability duplicates merge");
    try {
      capabilityDuplicateMigrationService.migrate(
        "organizations_acquisition_units_assignment.create",
        "ui-organizations_acqunits.execute");
      log.info("Capability duplicates merge completed successfully");
    } catch (Exception e) {
      log.warn("Error during capability duplicates merge", e);
    }
  }
}
