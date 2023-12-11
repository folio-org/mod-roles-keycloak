package org.folio.roles.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.Capabilities;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.rest.resource.CapabilityApi;
import org.folio.roles.service.capability.CapabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CapabilityController implements CapabilityApi {

  private final CapabilityService capabilityService;

  @Override
  public ResponseEntity<Capability> getCapabilityById(UUID id) {
    var capability = capabilityService.get(id);
    return ResponseEntity.ok(capability);
  }

  @Override
  public ResponseEntity<Capabilities> findCapabilities(String query, Integer limit, Integer offset) {
    var pageResult = capabilityService.find(query, limit, offset);
    return ResponseEntity.ok(new Capabilities()
      .capabilities(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }

  @Override
  public ResponseEntity<Capabilities> findCapabilitiesByCapabilitySetId(UUID id, Integer limit, Integer offset) {
    var pageResult = capabilityService.findByCapabilitySetId(id, limit, offset);
    return ResponseEntity.ok(new Capabilities()
      .capabilities(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }
}
