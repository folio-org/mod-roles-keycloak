package org.folio.roles.integration.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.recover.ModuleIdExtractor;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.roles.integration.kafka.model.CapabilityEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Component
@RequiredArgsConstructor
public class CapabilityEventModuleIdExtractor implements ModuleIdExtractor {

  private final ObjectMapper objectMapper;

  @Override
  public String apply(ResourceEvent<?> resourceEvent) {
    var value = resourceEvent.getNewValue() != null ? resourceEvent.getNewValue() : resourceEvent.getOldValue();
    try {
      var capabilityEvent = objectMapper.convertValue(value, CapabilityEvent.class);
      return capabilityEvent.getModuleId();
    } catch (IllegalArgumentException e) {
      log.info("Failed to extract moduleId from resource event: record = {}, exception = {}. Returning null",
        resourceEvent, e.getMessage());
      return null;
    }
  }
}
