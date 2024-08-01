package org.folio.roles.support;

import static java.time.Month.JANUARY;
import static java.time.Month.SEPTEMBER;
import static java.time.ZoneOffset.UTC;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.domain.dto.PolicyType.TIME;
import static org.folio.roles.domain.dto.PolicyType.USER;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.RolePolicy;
import org.folio.roles.domain.dto.RolePolicyRole;
import org.folio.roles.domain.dto.TimePolicy;
import org.folio.roles.domain.dto.TimePolicy.LogicEnum;
import org.folio.roles.domain.dto.UserPolicy;

@UtilityClass
public class PolicyUtils {

  public static final String POLICY_NAME = "policy-name";
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

  public static Policy rolePolicy() {
    return rolePolicy(POLICY_NAME);
  }

  public static Policy rolePolicy(String name) {
    return new Policy()
      .id(POLICY_ID)
      .name(name)
      .description(POLICY_DESCRIPTION)
      .type(ROLE)
      .rolePolicy(new RolePolicy().addRolesItem(new RolePolicyRole().id(ROLE_ID)));
  }

  public static Policy createTimePolicy() {
    var policy = new Policy();
    policy.setId(POLICY_ID);
    policy.setName(POLICY_NAME);
    policy.setDescription(POLICY_DESCRIPTION);
    policy.setType(TIME);
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
    timePolicy.setLogic(LogicEnum.POSITIVE);
    policy.setTimePolicy(timePolicy);
    return policy;
  }

  public static Policy userPolicy() {
    return userPolicy(POLICY_NAME);
  }

  public static Policy userPolicy(String name) {
    return basePolicy(name)
      .id(POLICY_ID)
      .system(false)
      .type(USER)
      .userPolicy(new UserPolicy().addUsersItem(USER_ID));
  }

  public static org.folio.roles.integration.keyclock.model.policy.UserPolicy createResponseKeycloakUserPolicy() {
    var userPolicy = new org.folio.roles.integration.keyclock.model.policy.UserPolicy();
    userPolicy.setUsers(List.of(USER_ID));
    userPolicy.setId(POLICY_ID);
    userPolicy.setName(POLICY_NAME);
    userPolicy.setDescription(POLICY_DESCRIPTION);
    userPolicy.setType(USER.name());
    return userPolicy;
  }

  public static org.folio.roles.integration.keyclock.model.policy.TimePolicy createKeycloakTimePolicy() {
    var timePolicy = new org.folio.roles.integration.keyclock.model.policy.TimePolicy();
    timePolicy.setNotBefore(LocalDateTime.of(2022, JANUARY, 10, 0, 0));
    timePolicy.setNotOnOrAfter(LocalDateTime.of(2022, SEPTEMBER, 10, 0, 0));
    timePolicy.setMonth(MONTH_START);
    timePolicy.setMonthEnd(MONTH_END);
    timePolicy.setDayMonth(DAY_OF_MONTH_START);
    timePolicy.setDayMonthEnd(DAY_OF_MONTH_END);
    timePolicy.setHour(HOUR_START);
    timePolicy.setHourEnd(HOUR_END);
    timePolicy.setMinute(MINUTE_START);
    timePolicy.setMinuteEnd(MINUTE_END);
    timePolicy.setId(POLICY_ID);
    timePolicy.setName(POLICY_NAME);
    timePolicy.setDescription(POLICY_DESCRIPTION);
    timePolicy.setLogic(LogicEnum.POSITIVE.name());
    timePolicy.setType(TIME.name());
    return timePolicy;
  }

  public static org.folio.roles.integration.keyclock.model.policy.TimePolicy createKeycloakTimePolicyWithConfig() {
    var config = new org.folio.roles.integration.keyclock.model.policy.TimePolicy.Config();
    config.setNbf(NOT_BEFORE);
    config.setNoa(NOT_ON_OR_AFTER);
    config.setDayMonth(DAY_OF_MONTH_START);
    config.setDayMonthEnd(DAY_OF_MONTH_END);
    config.setMonth(MONTH_START);
    config.setMonthEnd(MONTH_END);
    config.setHour(HOUR_START);
    config.setHourEnd(HOUR_END);
    config.setMinute(MINUTE_START);
    config.setMinuteEnd(MINUTE_END);
    var timePolicy = new org.folio.roles.integration.keyclock.model.policy.TimePolicy();
    timePolicy.setConfig(config);
    timePolicy.setId(POLICY_ID);
    timePolicy.setName(POLICY_NAME);
    timePolicy.setDescription(POLICY_DESCRIPTION);
    timePolicy.setLogic(LogicEnum.POSITIVE.name());
    timePolicy.setType(TIME.getValue());
    return timePolicy;
  }

  private static Policy basePolicy(String name) {
    return new Policy()
      .id(POLICY_ID)
      .name(name)
      .description(POLICY_DESCRIPTION);
  }
}
