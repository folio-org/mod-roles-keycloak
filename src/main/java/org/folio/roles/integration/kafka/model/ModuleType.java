package org.folio.roles.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ModuleType {

  MODULE("module"),
  UI_MODULE("ui-module");

  @JsonValue
  private final String value;
}
