package org.folio.roles.support;

import static java.time.Instant.ofEpochSecond;
import static java.time.Month.JANUARY;
import static java.time.Month.SEPTEMBER;
import static java.time.ZoneOffset.UTC;
import static java.util.Map.entry;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.domain.dto.PolicyType.TIME;
import static org.folio.roles.support.KeycloakUserUtils.KEYCLOAK_USER_ID;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyLogicType;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.SourceType;
import org.folio.roles.domain.dto.TimePolicy;
import org.folio.roles.domain.dto.UserPolicy;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;

@UtilityClass
public class PolicyUtils {

  public static final String POLICY_NAME = "policy-name";
  public static final String USER_POLICY_NAME = "Test user policy";
  public static final String ROLE_POLICY_NAME = "Test role policy";
  public static final String TIME_POLICY_NAME = "Test time policy";

  public static final String POLICY_DESCRIPTION = "policy description";
  public static final LocalDateTime NOT_BEFORE = LocalDateTime.of(2022, JANUARY, 10, 0, 0);
  public static final Date START = Date.from(NOT_BEFORE.toInstant(UTC));
  public static final LocalDateTime NOT_ON_OR_AFTER = LocalDateTime.of(2022, SEPTEMBER, 10, 0, 0);
  public static final Date EXPIRE = Date.from(NOT_ON_OR_AFTER.toInstant(UTC));
  public static final int DAY_OF_MONTH_START = 1;
  public static final int DAY_OF_MONTH_END = 3;
  public static final int MONTH_START = 1;
  public static final int MONTH_END = 3;
  public static final int HOUR_START = 1;
  public static final int HOUR_END = 3;
  public static final int MINUTE_START = 1;
  public static final int MINUTE_END = 3;
  public static final UUID POLICY_ID = UUID.randomUUID();

  public static Policy createTimePolicy() {
    var policy = new Policy();
    policy.setId(POLICY_ID);
    policy.setName(POLICY_NAME);
    policy.setDescription(POLICY_DESCRIPTION);
    policy.setType(TIME);
    policy.setSource(SourceType.USER);
    var timePolicy = new TimePolicy();
    timePolicy.repeat(true);
    timePolicy.setStart(START);
    timePolicy.setExpires(EXPIRE);
    timePolicy.setDayOfMonthStart(DAY_OF_MONTH_START);
    timePolicy.setDayOfMonthEnd(DAY_OF_MONTH_END);
    timePolicy.setMonthStart(MONTH_START);
    timePolicy.setMonthEnd(MONTH_END);
    timePolicy.setHourStart(HOUR_START);
    timePolicy.setHourEnd(HOUR_END);
    timePolicy.setMinuteStart(MINUTE_START);
    timePolicy.setMinuteEnd(MINUTE_END);
    timePolicy.setLogic(PolicyLogicType.POSITIVE);
    policy.setTimePolicy(timePolicy);
    return policy;
  }

  public static Policy userPolicy() {
    return userPolicy(USER_POLICY_NAME, List.of(USER_ID));
  }

  public static Policy userPolicy(String name) {
    return userPolicy(name, List.of(USER_ID));
  }

  public static Policy userPolicy(List<UUID> userIds) {
    return userPolicy(USER_POLICY_NAME, userIds);
  }

  public static Policy userPolicy(String name, List<UUID> userIds) {
    return new Policy()
      .id(POLICY_ID)
      .name(name)
      .description("Test user policy description")
      .type(PolicyType.USER)
      .userPolicy(new UserPolicy()
        .logic(PolicyLogicType.POSITIVE)
        .users(userIds)
      );
  }

  public static Policy rolePolicy() {
    return new Policy()
      .id(POLICY_ID)
      .name(ROLE_POLICY_NAME)
      .description("Test role policy description")
      .type(PolicyType.ROLE)
      .rolePolicy(new RolePolicy()
        .logic(PolicyLogicType.POSITIVE)
        .roles(List.of(new RolePolicyRole().id(ROLE_ID).required(false)))
      );
  }

  public static Policy rolePolicy(String name) {
    return new Policy()
      .id(POLICY_ID)
      .name(name)
      .description(POLICY_DESCRIPTION)
      .type(ROLE)
      .rolePolicy(new RolePolicy().addRolesItem(new RolePolicyRole().id(ROLE_ID)));
  }

  public static Policy timePolicy() {
    return new Policy()
      .id(POLICY_ID)
      .name(TIME_POLICY_NAME)
      .description("Test time policy description")
      .type(PolicyType.TIME)
      .timePolicy(new TimePolicy()
        .logic(PolicyLogicType.POSITIVE)
        .repeat(true)
        .start(Date.from(ofEpochSecond(1724716800)))
        .expires(Date.from(ofEpochSecond(1724976000)))
        .monthStart(1)
        .monthEnd(3)
      );
  }

  public static PolicyRepresentation keycloakTimePolicy() {
    return keycloakTimePolicy(POLICY_ID);
  }

  public static PolicyRepresentation keycloakTimePolicy(UUID id) {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setId(id.toString());
    policyRepresentation.setName("Test time policy");
    policyRepresentation.setDescription("Test time policy description");
    policyRepresentation.setType("time");
    policyRepresentation.setLogic(Logic.POSITIVE);
    policyRepresentation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
    policyRepresentation.setConfig(Map.ofEntries(
      entry("nbf", "2024-08-27 00:00:00"),
      entry("noa", "2024-08-30 00:00:00"),
      entry("minute", ""),
      entry("minuteEnd", ""),
      entry("hour", ""),
      entry("hourEnd", ""),
      entry("month", "1"),
      entry("monthEnd", "3"),
      entry("dayMonth", ""),
      entry("dayMonthEnd", "")
    ));

    return policyRepresentation;
  }

  public static PolicyRepresentation keycloakRolePolicy() {
    return keycloakRolePolicy(POLICY_ID);
  }

  public static PolicyRepresentation keycloakRolePolicy(UUID id) {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setId(id.toString());
    policyRepresentation.setName("Test role policy");
    policyRepresentation.setDescription("Test role policy description");
    policyRepresentation.setType("role");
    policyRepresentation.setLogic(Logic.POSITIVE);
    policyRepresentation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
    policyRepresentation.setConfig(Map.of(
      "roles", "[{\"id\":\"" + ROLE_ID + "\",\"required\":false}]",
      "fetchRoles", "false"
    ));

    return policyRepresentation;
  }

  public static PolicyRepresentation keycloakUserPolicy() {
    return keycloakUserPolicy(POLICY_ID);
  }

  public static PolicyRepresentation keycloakUserPolicy(UUID id) {
    var policyRepresentation = new PolicyRepresentation();
    policyRepresentation.setId(id.toString());
    policyRepresentation.setName("Test user policy");
    policyRepresentation.setDescription("Test user policy description");
    policyRepresentation.setType("user");
    policyRepresentation.setLogic(Logic.POSITIVE);
    policyRepresentation.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
    policyRepresentation.setConfig(Map.of("users", "[\"" + KEYCLOAK_USER_ID + "\"]]"));

    return policyRepresentation;
  }
}
