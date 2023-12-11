package org.folio.roles.controller;

import static org.springframework.http.HttpStatus.CREATED;

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
  public ResponseEntity<CapabilitySet> createCapabilitySet(CapabilitySet capability) {
    var createdCapability = capabilitySetService.create(capability);
    return ResponseEntity.status(CREATED).body(createdCapability);
  }

  @Override
  public ResponseEntity<CapabilitySets> createCapabilitySets(CapabilitySets capabilitySets) {
    var createdCapabilitySets = capabilitySetService.create(capabilitySets.getCapabilitySets());
    return ResponseEntity.status(CREATED).body(new CapabilitySets()
      .capabilitySets(createdCapabilitySets)
      .totalRecords((long) createdCapabilitySets.size()));
  }

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

  @Override
  public ResponseEntity<Void> updateCapabilitySet(UUID id, CapabilitySet capabilitySet) {
    capabilitySetService.update(id, capabilitySet);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteCapabilitySet(UUID id) {
    capabilitySetService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
