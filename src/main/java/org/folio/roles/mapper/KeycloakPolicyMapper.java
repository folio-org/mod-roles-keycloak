package org.folio.roles.mapper;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.anyNotNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.roles.utils.ParseUtils.parseDateSafe;
import static org.folio.roles.utils.ParseUtils.parseIntSafe;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import jakarta.annotation.Resource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyLogicType;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.TimePolicy;
import org.folio.roles.utils.JsonHelper;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import tools.jackson.core.type.TypeReference;

/**
 * KeycloakPolicyMapper is a mapper class that maps between Keycloak's policy model and the application's policy model.
 */
@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, imports = PolicyType.class)
public abstract class KeycloakPolicyMapper {

  private static final String FETCH_ROLES_CONFIG_PROPERTY = "fetchRoles";

  @Resource private JsonHelper jsonHelper;

  /**
   * Maps a TimePolicy object from Keycloak to a Policy object used by the application.
   *
   * @param kcPolicy the {@link TimePolicy} object to be mapped.
   * @return the mapped {@link Policy} object.
   */
  @Mapping(target = "id", source = "kcPolicy.id")
  @Mapping(target = "description", source = "kcPolicy.description")
  @Mapping(target = "name", source = "kcPolicy.name")
  @Mapping(target = "type", expression = "java(PolicyType.TIME)")
  @Mapping(target = "source", ignore = true)
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "userPolicy", ignore = true)
  @Mapping(target = "rolePolicy", ignore = true)
  public abstract Policy toTimePolicy(PolicyRepresentation kcPolicy, TimePolicy timePolicy);

  /**
   * Maps a RolePolicy object from Keycloak to a Policy object used by the application.
   *
   * @param kcPolicy the {@link TimePolicy} object to be mapped.
   * @return the mapped {@link Policy} object.
   */
  @Mapping(target = "id", source = "kcPolicy.id")
  @Mapping(target = "description", source = "kcPolicy.description")
  @Mapping(target = "name", source = "kcPolicy.name")
  @Mapping(target = "type", expression = "java(PolicyType.ROLE)")
  @Mapping(target = "rolePolicy.roles", source = "rolePolicyRoles")
  @Mapping(target = "source", ignore = true)
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "userPolicy", ignore = true)
  @Mapping(target = "timePolicy", ignore = true)
  public abstract Policy toRolePolicy(PolicyRepresentation kcPolicy, List<RolePolicyRole> rolePolicyRoles);

  /**
   * Maps a RolePolicy object from Keycloak to a Policy object used by the application.
   *
   * @param kcPolicy the {@link TimePolicy} object to be mapped.
   * @return the mapped {@link Policy} object.
   */
  @Mapping(target = "id", source = "kcPolicy.id")
  @Mapping(target = "description", source = "kcPolicy.description")
  @Mapping(target = "name", source = "kcPolicy.name")
  @Mapping(target = "type", expression = "java(PolicyType.USER)")
  @Mapping(target = "userPolicy.users", source = "userIds")
  @Mapping(target = "source", ignore = true)
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "rolePolicy", ignore = true)
  @Mapping(target = "timePolicy", ignore = true)
  public abstract Policy toUserPolicy(PolicyRepresentation kcPolicy, List<UUID> userIds);

  /**
   * Maps a Policy object used by the application to a TimePolicy object used by Keycloak.
   *
   * @param source the {@link Policy} object to be mapped.
   * @return the mapped {@link PolicyRepresentation} object.
   */
  @KeycloakPolicyIgnoredFieldMappings
  @Mapping(target = "id", source = "source.id")
  @Mapping(target = "name", source = "source.name")
  @Mapping(target = "description", source = "source.description")
  @Mapping(target = "type", expression = "java(\"time\")")
  @Mapping(target = "logic", source = "source.timePolicy.logic")
  @Mapping(target = "config", source = "config")
  public abstract PolicyRepresentation toKeycloakTimePolicy(Policy source, Map<String, String> config);

  /**
   * Maps a {@link Policy} object to a {@link RolePolicyRepresentation} object.
   *
   * @param source the source {@link Policy} object
   * @return the mapped {@link PolicyRepresentation} object
   */
  @KeycloakPolicyIgnoredFieldMappings
  @Mapping(target = "id", source = "source.id")
  @Mapping(target = "name", source = "source.name")
  @Mapping(target = "description", source = "source.description")
  @Mapping(target = "type", expression = "java(\"role\")")
  @Mapping(target = "config", source = "config")
  @Mapping(target = "logic", source = "source.rolePolicy.logic")
  public abstract PolicyRepresentation toKeycloakRolePolicy(Policy source, Map<String, String> config);

  /**
   * Maps a {@link Policy} object to a {@link UserPolicyRepresentation} subclass.
   *
   * @param source the source {@link Policy} object
   * @return the mapped {@link PolicyRepresentation} object
   */
  @KeycloakPolicyIgnoredFieldMappings
  @Mapping(target = "id", source = "source.id")
  @Mapping(target = "name", source = "source.name")
  @Mapping(target = "description", source = "source.description")
  @Mapping(target = "type", expression = "java(\"user\")")
  @Mapping(target = "config", source = "config")
  @Mapping(target = "logic", source = "source.userPolicy.logic")
  @Mapping(target = "decisionStrategy", ignore = true)
  public abstract PolicyRepresentation toKeycloakUserPolicy(Policy source, Map<String, String> config);

  /**
   * Maps a {@link Policy} object to a {@link Policy} object.
   *
   * @param policyRepresentation the source {@link Policy} object
   * @return the mapped {@link Policy} object
   * @throws UnsupportedOperationException if the source object is not a known subclass
   */
  public Policy toPolicy(PolicyRepresentation policyRepresentation) {
    if (policyRepresentation == null || policyRepresentation.getConfig() == null) {
      return null;
    }

    var config = policyRepresentation.getConfig();
    var policyType = policyRepresentation.getType();
    return switch (policyType) {
      case "role" -> toRolePolicy(policyRepresentation, parseRoleIds(config));
      case "user" -> toUserPolicy(policyRepresentation, parseUserIds(config));
      case "time" -> toTimePolicy(policyRepresentation, parseTimePolicy(config));
      default -> throw new UnsupportedOperationException("Mapper not defined for source type: " + policyType);
    };
  }

  /**
   * Maps a {@link Policy} object to its corresponding {@link Policy} subclass. The mapping is based on the "type"
   * field of the source object, which can be one of: {@link PolicyType#TIME}, {@link PolicyType#ROLE},
   * {@link PolicyType#USER}.
   *
   * @param source the source {@link Policy} object
   * @return the mapped {@link Policy} object
   * @throws UnsupportedOperationException if the source object is not a known subclass
   */
  public PolicyRepresentation toKeycloakPolicy(Policy source, PolicyMapperContext mappingContext) {
    if (source == null || mappingContext == null) {
      return null;
    }

    return switch (source.getType()) {
      case TIME -> toKeycloakTimePolicy(source, getTimePolicyConfig(source.getTimePolicy()));
      case ROLE -> toKeycloakRolePolicy(source, getRolePolicyConfig(source.getRolePolicy()));
      case USER -> toKeycloakUserPolicy(source, getUserPolicyConfig(mappingContext.getKeycloakUserIds()));
      default -> throw new UnsupportedOperationException(
        "Mapper not defined: source type = " + source.getClass().getSimpleName());
    };
  }

  @AfterMapping
  public void setTimePolicyLogic(PolicyRepresentation policyRepresentation, @MappingTarget Policy policy) {
    if (policy.getTimePolicy() != null) {
      policy.getTimePolicy().setLogic(parsePolicyLogic(policyRepresentation));
      return;
    }

    if (policy.getRolePolicy() != null) {
      policy.getRolePolicy().setLogic(parsePolicyLogic(policyRepresentation));
      return;
    }

    if (policy.getUserPolicy() != null) {
      policy.getUserPolicy().setLogic(parsePolicyLogic(policyRepresentation));
    }
  }

  private static Map<String, String> getTimePolicyConfig(TimePolicy timePolicy) {
    return Map.ofEntries(
      Map.entry("nbf", EMPTY),
      Map.entry("noa", EMPTY),
      Map.entry("minute", getIntegerAsStringOrEmpty(timePolicy.getMinuteStart())),
      Map.entry("minuteEnd", getIntegerAsStringOrEmpty(timePolicy.getMinuteEnd())),
      Map.entry("hour", getIntegerAsStringOrEmpty(timePolicy.getHourStart())),
      Map.entry("hourEnd", getIntegerAsStringOrEmpty(timePolicy.getHourEnd())),
      Map.entry("dayMonth", getIntegerAsStringOrEmpty(timePolicy.getDayOfMonthStart())),
      Map.entry("dayMonthEnd", getIntegerAsStringOrEmpty(timePolicy.getDayOfMonthEnd())),
      Map.entry("month", getIntegerAsStringOrEmpty(timePolicy.getMonthStart())),
      Map.entry("monthEnd", getIntegerAsStringOrEmpty(timePolicy.getMonthEnd()))
    );
  }

  private Map<String, String> getUserPolicyConfig(List<String> userIds) {
    return userIds == null
      ? emptyMap()
      : Map.of(
        "users", jsonHelper.asJsonStringSafe(userIds),
        FETCH_ROLES_CONFIG_PROPERTY, "true");
  }

  private Map<String, String> getRolePolicyConfig(RolePolicy rolePolicy) {
    return rolePolicy == null
      ? emptyMap()
      : Map.of(
        "roles", jsonHelper.asJsonStringSafe(rolePolicy.getRoles()),
        FETCH_ROLES_CONFIG_PROPERTY, "true");
  }

  private List<UUID> parseUserIds(Map<String, String> config) {
    return jsonHelper.parse(emptyIfNull(config).get("users"), new TypeReference<>() {});
  }

  private List<RolePolicyRole> parseRoleIds(Map<String, String> config) {
    return jsonHelper.parse(emptyIfNull(config).get("roles"), new TypeReference<>() {});
  }

  private static TimePolicy parseTimePolicy(Map<String, String> config) {
    var timePolicyConfig = emptyIfNull(config);

    return new TimePolicy()
      .start(parseDateSafe(timePolicyConfig.get("nbf")))
      .expires(parseDateSafe(timePolicyConfig.get("noa")))
      .minuteStart(parseIntSafe(timePolicyConfig.get("minute")))
      .minuteEnd(parseIntSafe(timePolicyConfig.get("minuteEnd")))
      .hourStart(parseIntSafe(timePolicyConfig.get("hour")))
      .hourEnd(parseIntSafe(timePolicyConfig.get("hourEnd")))
      .dayOfMonthStart(parseIntSafe(timePolicyConfig.get("dayMonth")))
      .dayOfMonthEnd(parseIntSafe(timePolicyConfig.get("dayMonthEnd")))
      .monthStart(parseIntSafe(timePolicyConfig.get("month")))
      .monthEnd(parseIntSafe(timePolicyConfig.get("monthEnd")))
      .repeat(isRepeatedTimePolicy(config));
  }

  protected static boolean isRepeatedTimePolicy(Map<String, String> policyConfig) {
    var minuteStart = parseIntSafe(policyConfig.get("minute"));
    var hourStart = parseIntSafe(policyConfig.get("hour"));
    var dayStart = parseIntSafe(policyConfig.get("dayMonth"));
    var monthStart = parseIntSafe(policyConfig.get("month"));
    return anyNotNull(hourStart, monthStart, dayStart, minuteStart);
  }

  protected static PolicyLogicType parsePolicyLogic(PolicyRepresentation policyRepresentation) {
    return ofNullable(policyRepresentation)
      .map(AbstractPolicyRepresentation::getLogic)
      .map(Enum::name)
      .map(enumValue -> PolicyLogicType.fromValue(enumValue.toUpperCase()))
      .orElse(null);
  }

  private static String getIntegerAsStringOrEmpty(Integer integer) {
    return integer != null ? integer.toString() : "";
  }

  @Retention(RetentionPolicy.CLASS)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "scopes", ignore = true)
  @Mapping(target = "policies", ignore = true)
  @Mapping(target = "resources", ignore = true)
  @Mapping(target = "scopesData", ignore = true)
  @Mapping(target = "resourcesData", ignore = true)
  @interface KeycloakPolicyIgnoredFieldMappings {}

  @Data
  @NoArgsConstructor
  public static class PolicyMapperContext {

    private List<String> keycloakUserIds;

    /**
     * Creates an empty {@link PolicyMapperContext} object.
     */
    public static PolicyMapperContext empty() {
      return new PolicyMapperContext();
    }

    /**
     * Sets keycloakUserIds for {@link PolicyMapperContext} and returns {@link PolicyMapperContext}.
     *
     * @return this {@link PolicyMapperContext} with new keycloakUserIds value
     */
    public PolicyMapperContext keycloakUserIds(List<String> keycloakUserIds) {
      this.keycloakUserIds = keycloakUserIds;
      return this;
    }
  }
}
