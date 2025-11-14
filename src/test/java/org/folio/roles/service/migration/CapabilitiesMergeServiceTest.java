package org.folio.roles.service.migration;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CapabilitiesMergeServiceTest {

  @Mock private CapabilityDuplicateMigrationService capabilityDuplicateMigrationService;

  @InjectMocks private CapabilitiesMergeService service;

  @Test
  void mergeDuplicateCapabilities_positive() {
    service.mergeDuplicateCapabilities();

    verify(capabilityDuplicateMigrationService).migrate(
      "organizations_acquisition_units_assignment.create",
      "ui-organizations_acqunits.execute");
  }

  @Test
  void mergeDuplicateCapabilities_positive_handlesMigrationException() {
    doThrow(new RuntimeException("Migration failed")).when(capabilityDuplicateMigrationService)
      .migrate("organizations_acquisition_units_assignment.create", "ui-organizations_acqunits.execute");

    // Should not throw - exceptions are caught and logged
    service.mergeDuplicateCapabilities();

    verify(capabilityDuplicateMigrationService).migrate(
      "organizations_acquisition_units_assignment.create",
      "ui-organizations_acqunits.execute");
  }
}
