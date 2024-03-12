package org.folio.roles.controller;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.CapabilitySet;
import org.folio.roles.domain.dto.CapabilitySets;
import org.folio.roles.rest.resource.CapabilitySetApi;
import org.folio.roles.service.capability.CapabilitySetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CapabilitySetController implements CapabilitySetApi {

  private final CapabilitySetService capabilitySetService;

  @Override
  public ResponseEntity<CapabilitySet> getCapabilitySetById(UUID id) {
    var result = capabilitySetService.get(id);
    return ResponseEntity.ok(result);
  }

  @Override
  public ResponseEntity<CapabilitySets> findCapabilitySets(String query, Integer limit, Integer offset) {
    var pageResult = capabilitySetService.find(query, limit, offset);
    return ResponseEntity.ok(new CapabilitySets()
      .capabilitySets(pageResult.getRecords())
      .totalRecords(pageResult.getTotalRecords()));
  }
}
