package org.folio.roles.integration.kafka.model;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.folio.roles.domain.dto.CapabilityType;

@RequiredArgsConstructor
public enum CapabilityAction {

  VIEW("view", List.of("get", "view", "read", "get-all", "read-all"), emptyList()),

  CREATE("create", List.of("post", "create", "write"), emptyList()),

  EDIT("edit", List.of("put", "edit", "update", "patch"), emptyList()),

  DELETE("delete", List.of("delete", "delete-all"), emptyList()),

  MANAGE("manage", List.of("all", "manage", "allops"), emptyList()),

  EXECUTE("execute", emptyList(), List.of("post", "download", "export", "assign", "restore", "approve", "reopen",
    "start", "unopen", "validate", "resend", "run-jobs", "stop-jobs", "generate", "validate", "reset", "test",
    "import", "cancel", "exportCSV", "showHidden", "updateEncumbrances"));

  public static final Set<String> DATA_SUFFIXES = getSuffixesByFunc(e -> e.dataSettingsActions);
  public static final Set<String> PROCEDURAL_KEYWORDS = getSuffixesByFunc(e -> e.proceduralActions);

  @Getter
  @JsonValue
  private final String value;

  @JsonIgnore
  private final List<String> dataSettingsActions;

  @JsonIgnore
  private final List<String> proceduralActions;

  @SuppressWarnings("UnnecessaryDefault")
  public static CapabilityAction getActionByString(String action, CapabilityType type) {
    return switch (type) {
      case PROCEDURAL -> getByMatchingValues(action, capabilityAction -> capabilityAction.proceduralActions);
      case SETTINGS, DATA -> getByMatchingValues(action, capabilityAction -> capabilityAction.dataSettingsActions);
      default -> null;
    };
  }

  private static Set<String> getSuffixesByFunc(Function<CapabilityAction, List<String>> mapper) {
    return Arrays.stream(CapabilityAction.values())
      .map(mapper)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  private static CapabilityAction getByMatchingValues(String action,
    Function<CapabilityAction, List<String>> allowedActionsProvider) {
    for (var capabilityAction : CapabilityAction.values()) {
      if (allowedActionsProvider.apply(capabilityAction).contains(action)) {
        return capabilityAction;
      }
    }
    return null;
  }
}
