package org.folio.roles.integration.kafka;

import static java.util.Collections.emptySet;
import static java.util.Map.entry;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.folio.roles.domain.dto.CapabilityAction.CREATE;
import static org.folio.roles.domain.dto.CapabilityAction.DELETE;
import static org.folio.roles.domain.dto.CapabilityAction.EDIT;
import static org.folio.roles.domain.dto.CapabilityAction.EXECUTE;
import static org.folio.roles.domain.dto.CapabilityAction.MANAGE;
import static org.folio.roles.domain.dto.CapabilityAction.VIEW;
import static org.folio.roles.domain.dto.CapabilityType.DATA;
import static org.folio.roles.domain.dto.CapabilityType.PROCEDURAL;
import static org.folio.roles.domain.dto.CapabilityType.SETTINGS;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.roles.domain.dto.Capability;
import org.folio.roles.domain.dto.CapabilityAction;
import org.folio.roles.domain.dto.CapabilityType;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CapabilityConverterUtils {

  private static final Map<CapabilityAction, Collection<String>> DATA_SETTINGS_ACTIONS = Map.ofEntries(
    entry(VIEW, List.of("get", "view", "read", "get-all", "read-all", "search")),
    entry(CREATE, List.of("post", "create", "write")),
    entry(EDIT, List.of("put", "edit", "update", "patch")),
    entry(DELETE, List.of("delete", "delete-all")),
    entry(MANAGE, List.of("all", "manage", "allops")));

  private static final Map<String, Capability> PERMISSION_MAPPING_OVERRIDES = getFolioPermissionMappingsOverrides();
  private static final List<String> DATA_KEYWORD_IDENTIFIERS = List.of("item", "collection", "items");
  private static final Set<String> DATA_SUFFIXES = Set.of(".item.post");
  private static final List<String> SETTINGS_KEYWORDS = List.of("module", "settings");
  private static final Collection<String> PROCEDURAL_KEYWORDS = Set.of(
    "post", "download", "export", "assign", "restore", "approve", "reopen", "start", "unopen", "validate",
    "resend", "run-jobs", "stop-jobs", "generate", "reset", "test", "import", "cancel", "exportCSV",
    "showHidden", "updateEncumbrances", "execute", "move");

  /**
   * Creates raw {@link Capability} object from permission name.
   *
   * <p>Raw object will contain only identifier fields: resource, type, action</p>
   *
   * @param permissionName - permission name to process
   * @return created raw {@link Capability} object
   */
  public static Capability getRawCapability(String permissionName) {
    var capability = PERMISSION_MAPPING_OVERRIDES.get(permissionName);
    if (capability != null) {
      return capability;
    }

    var permissionParts = StringUtils.split(permissionName, ".");
    var capabilityType = getCapabilityType(permissionName, permissionParts);
    var resourceActionEntry = getCapabilityResourceAndAction(permissionParts, capabilityType);
    var resource = resourceActionEntry.getKey();
    var action = resourceActionEntry.getValue();

    return new Capability()
      .resource(resource)
      .type(capabilityType)
      .action(action)
      .permission(permissionName);
  }

  /**
   * Check required fields for capability and logs it if it is missing.
   *
   * @param capability - {@link Capability} object to analyze
   * @return true if capability contains required fields, false - otherwise
   */
  public static boolean hasRequiredFields(Capability capability) {
    var permissionName = capability.getPermission();
    if (capability.getType() == null) {
      log.warn("Capability type is not resolved: permissionName = {}", permissionName);
      return false;
    }

    if (capability.getResource() == null) {
      log.warn("Capability resource is not resolved: permissionName = {}", permissionName);
      return false;
    }

    if (capability.getAction() == null) {
      log.warn("Capability action is not resolved: permissionName = {}", permissionName);
      return false;
    }

    return true;
  }

  private static CapabilityType getCapabilityType(String name, String[] permissionParts) {
    if (containsAny(permissionParts, SETTINGS_KEYWORDS) || startsWithAny(name, SETTINGS_KEYWORDS)) {
      return SETTINGS;
    }

    var containsDataKeywords = containsAny(permissionParts, DATA_KEYWORD_IDENTIFIERS);
    if (endWithAny(name, PROCEDURAL_KEYWORDS) && !containsDataKeywords) {
      return PROCEDURAL;
    }

    if (endWithAny(name, DATA_SUFFIXES)) {
      return DATA;
    }

    if (containsAny(permissionParts, PROCEDURAL_KEYWORDS)) {
      return PROCEDURAL;
    }

    return DATA;
  }

  private static Entry<String, CapabilityAction> getCapabilityResourceAndAction(String[] parts, CapabilityType type) {
    var length = parts.length;
    if (length <= 1) {
      return new SimpleImmutableEntry<>(null, null);
    }

    var capabilityAction = getCapabilityActionByString(parts[length - 1], type);
    if (type == PROCEDURAL) {
      var endIdx = capabilityAction != null ? length - 2 : length - 1;
      return new SimpleImmutableEntry<>(getResourceName(parts, endIdx), EXECUTE);
    }

    if (capabilityAction == null && type == SETTINGS) {
      return new SimpleImmutableEntry<>(getResourceName(parts, length - 1), VIEW);
    }

    return new SimpleImmutableEntry<>(getResourceName(parts, length - 2), capabilityAction);
  }

  private static String getResourceName(String[] parts, int endIdx) {
    var resourceNameBuilder = new StringJoiner(" ");
    for (int i = 0; i <= Math.min(parts.length - 1, endIdx); i++) {
      var nameParts = StringUtils.split(parts[i], "_");
      for (var namePart : nameParts) {
        resourceNameBuilder.add(toUpperKebabCase(namePart));
      }
    }

    var resultString = resourceNameBuilder.toString();
    return resultString.startsWith("Ui") ? "UI" + resultString.substring(2) : resultString;
  }

  public static CapabilityAction getCapabilityActionByString(String action, CapabilityType type) {
    return switch (type) {
      case SETTINGS, DATA -> getByMatchingValues(action, v -> DATA_SETTINGS_ACTIONS.getOrDefault(v, emptySet()));
      case PROCEDURAL -> getByMatchingValues(action, v -> v == EXECUTE ? PROCEDURAL_KEYWORDS : emptySet());
    };
  }

  private static CapabilityAction getByMatchingValues(String action,
    Function<CapabilityAction, Collection<String>> keywordProvider) {
    for (var capabilityAction : CapabilityAction.values()) {
      if (keywordProvider.apply(capabilityAction).contains(action)) {
        return capabilityAction;
      }
    }
    return null;
  }

  private static Map<String, Capability> getFolioPermissionMappingsOverrides() {
    var mappingOverridesFileLocation = "folio-permissions/mappings-overrides.json";
    try {
      var inStream = CapabilityConverterUtils.class.getClassLoader().getResourceAsStream(mappingOverridesFileLocation);
      return new ObjectMapper().readValue(inStream, new TypeReference<>() {});
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse mapping overrides file: " + mappingOverridesFileLocation, e);
    }
  }

  private static boolean containsAny(Object[] array, Collection<?> values) {
    for (var arrayValue : array) {
      if (values.contains(arrayValue)) {
        return true;
      }
    }

    return false;
  }

  private static boolean startsWithAny(String string, Collection<String> prefixes) {
    for (var prefix : prefixes) {
      if (string.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  private static boolean endWithAny(String string, Collection<String> prefixes) {
    for (var prefix : prefixes) {
      if (string.endsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  private static String toUpperKebabCase(String kebabCaseString) {
    if (!kebabCaseString.contains("-")) {
      return capitalize(kebabCaseString);
    }

    var result = new StringJoiner("-");
    for (String s : StringUtils.split(kebabCaseString, "-")) {
      result.add(capitalize(s));
    }

    return result.toString();
  }
}
