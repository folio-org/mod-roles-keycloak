package org.folio.roles.mapper;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.roles.domain.dto.Policies;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.integration.keyclock.model.policy.BasePolicy;
import org.folio.roles.integration.keyclock.model.policy.RolePolicy;
import org.folio.roles.integration.keyclock.model.policy.TimePolicy;
import org.folio.roles.integration.keyclock.model.policy.UserPolicy;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * KeycloakPolicyMapper is a mapper class that maps between Keycloak's policy model and the application's policy model.
 */
@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, imports = PolicyType.class)
public abstract class KeycloakPolicyMapper {

  @Resource
  private ObjectMapper objectMapper;

  /**
   * Maps a TimePolicy object from Keycloak to a Policy object used by the application.
   *
   * @param source the {@link TimePolicy} object to be mapped.
   * @return the mapped {@link Policy} object.
   */
  @Mapping(target = "timePolicy.start", source = "notBefore")
  @Mapping(target = "timePolicy.expires", source = "notOnOrAfter")
  @Mapping(target = "timePolicy.dayOfMonthStart", source = "dayMonth")
  @Mapping(target = "timePolicy.dayOfMonthEnd", source = "dayMonthEnd")
  @Mapping(target = "timePolicy.monthStart", source = "month")
  @Mapping(target = "timePolicy.monthEnd", source = "monthEnd")
  @Mapping(target = "timePolicy.hourStart", source = "hour")
  @Mapping(target = "timePolicy.hourEnd", source = "hourEnd")
  @Mapping(target = "timePolicy.minuteStart", source = "minute")
  @Mapping(target = "timePolicy.minuteEnd", source = "minuteEnd")
  @Mapping(target = "timePolicy.logic", source = "logic")
  @Mapping(target = "timePolicy.repeat", expression = "java(timePolicy.isRepeated())")
  @Mapping(target = "type", expression = "java(PolicyType.fromValue(source.getType().toUpperCase()))")
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "userPolicy", ignore = true)
  @Mapping(target = "rolePolicy", ignore = true)
  public abstract Policy toTimePolicy(TimePolicy source);

  /**
   * Maps a Policy object used by the application to a TimePolicy object used by Keycloak.
   *
   * @param source the {@link Policy} object to be mapped.
   * @return the mapped {@link TimePolicy} object.
   */
  @Mapping(target = "notBefore", source = "timePolicy.start")
  @Mapping(target = "notOnOrAfter", source = "timePolicy.expires")
  @Mapping(target = "dayMonth", source = "timePolicy.dayOfMonthStart")
  @Mapping(target = "dayMonthEnd", source = "timePolicy.dayOfMonthEnd")
  @Mapping(target = "month", source = "timePolicy.monthStart")
  @Mapping(target = "monthEnd", source = "timePolicy.monthEnd")
  @Mapping(target = "hour", source = "timePolicy.hourStart")
  @Mapping(target = "hourEnd", source = "timePolicy.hourEnd")
  @Mapping(target = "minute", source = "timePolicy.minuteStart")
  @Mapping(target = "minuteEnd", source = "timePolicy.minuteEnd")
  @Mapping(target = "logic", source = "timePolicy.logic")
  @Mapping(target = "config", ignore = true)
  @Mapping(target = "decisionStrategy", ignore = true)
  public abstract TimePolicy toKeycloakTimePolicy(Policy source);

  /**
   * Maps a {@link RolePolicy} object to a {@link Policy} object.
   *
   * @param source the source {@link RolePolicy} object
   * @return the mapped {@link Policy} object
   */
  @Mapping(target = "rolePolicy.logic", source = "logic")
  @Mapping(target = "rolePolicy.roles", source = "roles")
  @Mapping(target = "type", expression = "java(PolicyType.fromValue(source.getType().toUpperCase()))")
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "userPolicy", ignore = true)
  @Mapping(target = "timePolicy", ignore = true)
  public abstract Policy toRolePolicy(RolePolicy source);

  /**
   * Maps a {@link Policy} object to a {@link RolePolicy} object.
   *
   * @param source the source {@link Policy} object
   * @return the mapped {@link RolePolicy} object
   */
  @Mapping(target = "logic", source = "rolePolicy.logic")
  @Mapping(target = "roles", source = "rolePolicy.roles")
  @Mapping(target = "config", ignore = true)
  @Mapping(target = "decisionStrategy", ignore = true)
  public abstract RolePolicy toKeycloakRolePolicy(Policy source);

  /**
   * Maps a {@link UserPolicy} object to a {@link Policy} object.
   *
   * @param source the source {@link UserPolicy} object
   * @return the mapped {@link Policy} object
   */
  @Mapping(target = "userPolicy.logic", source = "logic")
  @Mapping(target = "userPolicy.users", source = "users")
  @Mapping(target = "type", expression = "java(PolicyType.fromValue(source.getType().toUpperCase()))")
  @Mapping(target = "metadata", ignore = true)
  @Mapping(target = "timePolicy", ignore = true)
  @Mapping(target = "rolePolicy", ignore = true)
  public abstract Policy toUserPolicy(UserPolicy source);

  /**
   * Maps a {@link Policy} object to a {@link UserPolicy} subclass.
   *
   * @param source the source {@link Policy} object
   * @return the mapped {@link BasePolicy} object
   */
  @Mapping(target = "logic", source = "userPolicy.logic")
  @Mapping(target = "users", source = "userPolicy.users")
  @Mapping(target = "config", ignore = true)
  @Mapping(target = "decisionStrategy", ignore = true)
  public abstract UserPolicy toKeycloakUserPolicy(Policy source);

  /**
   * Maps a {@link Policy} object to its corresponding {@link BasePolicy} subclass.
   * The mapping is based on the "type" field of the source object, which can be one of:
   * {@link PolicyType#TIME},
   * {@link PolicyType#ROLE},
   * {@link PolicyType#USER}.
   *
   * @param source the source {@link Policy} object
   * @return the mapped {@link BasePolicy} object
   * @throws UnsupportedOperationException if the source object is not a known subclass
   */
  public BasePolicy toKeycloakPolicy(Policy source) {
    if (isNull(source)) {
      return null;
    }
    return switch (source.getType()) {
      case TIME -> toKeycloakTimePolicy(source);
      case ROLE -> toKeycloakRolePolicy(source);
      case USER -> toKeycloakUserPolicy(source);
      default -> throw new UnsupportedOperationException(
        "Mapper not defined: source type = " + source.getClass().getSimpleName());
    };
  }

  /**
   * Maps a {@link BasePolicy} object to a {@link Policy} object.
   *
   * @param source the source {@link BasePolicy} object
   * @return the mapped {@link Policy} object
   * @throws UnsupportedOperationException if the source object is not a known subclass
   */
  public <S extends BasePolicy> Policy toPolicy(S source) {
    if (isNull(source)) {
      return null;
    }
    if (source instanceof TimePolicy policy) {
      return toTimePolicy(policy);
    } else if (source instanceof UserPolicy policy) {
      return toUserPolicy(policy);
    } else if (source instanceof RolePolicy policy) {
      return toRolePolicy(policy);
    }
    throw new UnsupportedOperationException("Mapper not defined: source type = " + source.getClass().getSimpleName());
  }

  /**
   * Maps a {@link List} of {@link BasePolicy} to a {@link Policies} object. Fills totalRecords property.
   *
   * @param policies the list of source {@link BasePolicy} objects
   * @return the mapped {@link Policies} object with count of total records
   */
  public Policies toPolicies(List<BasePolicy> policies) {
    var mappedPolicies = policies.stream().map(this::toPolicy).filter(Objects::isNull).collect(toList());
    if (isEmpty(mappedPolicies)) {
      return null;
    }
    return new Policies().policies(mappedPolicies).totalRecords(mappedPolicies.size());
  }

  /**
   * Method that is executed after mapping to convert role configuration if exists.
   *
   * @param rolePolicy the source {@link RolePolicy} object
   * @param policy     the mapped {@link Policy} object
   */
  @AfterMapping
  protected void convertRoleConfigIfExists(RolePolicy rolePolicy, @MappingTarget Policy policy) {
    if (rolePolicy.hasConfigValue()) {
      policy.getRolePolicy().setRoles(extractRoles(rolePolicy));
    }
  }

  /**
   * Method that is executed after mapping to convert user configuration if exists.
   *
   * @param userPolicy the source {@link UserPolicy} object
   * @param policy     the mapped {@link Policy} objec
   */
  @AfterMapping
  protected void convertUserConfigIfExists(UserPolicy userPolicy, @MappingTarget Policy policy) {
    if (userPolicy.hasConfigValue()) {
      policy.getUserPolicy().setUsers(extractUsersIds(userPolicy));
    }
  }

  /**
   * Method that is executed after mapping to convert time configuration if exists.
   *
   * @param timePolicy the source {@link TimePolicy} object
   * @param policy     the mapped {@link Policy} object
   */
  @AfterMapping
  protected void convertTimeConfigIfExists(TimePolicy timePolicy, @MappingTarget Policy policy) {
    if (timePolicy.hasConfigValue()) {
      var target = policy.getTimePolicy();
      var config = timePolicy.getConfig();
      target.setRepeat(timePolicy.isRepeated());
      target.setStart(Date.from(config.getNbf().toInstant(UTC)));
      target.setExpires(Date.from(config.getNoa().toInstant(UTC)));
      target.setDayOfMonthStart(config.getDayMonth());
      target.setDayOfMonthEnd(config.getDayMonthEnd());
      target.setMonthStart(config.getMonth());
      target.setMonthEnd(config.getMonthEnd());
      target.setHourStart(config.getHour());
      target.setHourEnd(config.getHourEnd());
      target.setMinuteStart(config.getMinute());
      target.setMinuteEnd(config.getMinuteEnd());
    }
  }

  @SneakyThrows
  private List<RolePolicyRole> extractRoles(RolePolicy source) {
    if (isNull(source.getConfig().getRoles())) {
      return emptyList();
    }
    return objectMapper.readValue(source.getConfig().getRoles(), new TypeReference<>() {});
  }

  @SneakyThrows
  private List<UUID> extractUsersIds(UserPolicy source) {
    return objectMapper.readValue(source.getConfig().getUsers(), new TypeReference<>() {});
  }
}
