package org.folio.roles.mapper.entity;

import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.roles.domain.dto.PolicyType.ROLE;
import static org.folio.roles.domain.dto.PolicyType.TIME;
import static org.folio.roles.domain.dto.PolicyType.USER;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.PolicyType;
import org.folio.roles.domain.entity.BasePolicyEntity;
import org.folio.roles.domain.entity.RolePolicyEntity;
import org.folio.roles.domain.entity.TimePolicyEntity;
import org.folio.roles.domain.entity.UserPolicyEntity;
import org.folio.roles.mapper.AuditableEntityMapping;
import org.folio.roles.mapper.AuditableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = CONSTRUCTOR, uses = DateConvertHelper.class)
public interface PolicyEntityMapper {

  @AuditableMapping
  @Mapping(target = "timePolicy", source = ".")
  @Mapping(target = "type", expression = "java(toPolicyType(entity))")
  @Mapping(target = "userPolicy", ignore = true)
  @Mapping(target = "rolePolicy", ignore = true)
  Policy toTimePolicy(TimePolicyEntity entity);

  @AuditableEntityMapping
  @Mapping(target = "start", source = "timePolicy.start")
  @Mapping(target = "expires", source = "timePolicy.expires")
  @Mapping(target = "dayOfMonthStart", source = "timePolicy.dayOfMonthStart")
  @Mapping(target = "dayOfMonthEnd", source = "timePolicy.dayOfMonthEnd")
  @Mapping(target = "monthStart", source = "timePolicy.monthStart")
  @Mapping(target = "monthEnd", source = "timePolicy.monthEnd")
  @Mapping(target = "hourStart", source = "timePolicy.hourStart")
  @Mapping(target = "hourEnd", source = "timePolicy.hourEnd")
  @Mapping(target = "minuteStart", source = "timePolicy.minuteStart")
  @Mapping(target = "minuteEnd", source = "timePolicy.minuteEnd")
  @Mapping(target = "logic", source = "timePolicy.logic")
  @Mapping(target = "source", source = "timePolicy.source")
  @Mapping(target = "repeat", source = "timePolicy.repeat")
  TimePolicyEntity toTimePolicyEntity(Policy policy);

  @AuditableMapping
  @Mapping(target = "userPolicy", source = ".")
  @Mapping(target = "type", expression = "java(toPolicyType(entity))")
  @Mapping(target = "timePolicy", ignore = true)
  @Mapping(target = "rolePolicy", ignore = true)
  Policy toUserPolicy(UserPolicyEntity entity);

  @AuditableEntityMapping
  @Mapping(target = "users", source = "userPolicy.users")
  @Mapping(target = "logic", source = "userPolicy.logic")
  @Mapping(target = "source", source = "userPolicy.source")
  UserPolicyEntity toUserPolicyEntity(Policy policy);

  @AuditableMapping
  @Mapping(target = "rolePolicy", source = ".")
  @Mapping(target = "type", expression = "java(toPolicyType(entity))")
  @Mapping(target = "userPolicy", ignore = true)
  @Mapping(target = "timePolicy", ignore = true)
  Policy toRolePolicy(RolePolicyEntity entity);

  @AuditableEntityMapping
  @Mapping(target = "logic", source = "rolePolicy.logic")
  @Mapping(target = "roles", source = "rolePolicy.roles")
  @Mapping(target = "source", source = "rolePolicy.source")
  RolePolicyEntity toRolePolicyEntity(Policy policy);

  default BasePolicyEntity toPolicyEntity(Policy source) {
    if (isNull(source)) {
      return null;
    }
    return switch (source.getType()) {
      case TIME -> toTimePolicyEntity(source);
      case ROLE -> toRolePolicyEntity(source);
      case USER -> toUserPolicyEntity(source);
      default -> throw new UnsupportedOperationException(
        "Mapper not defined: source type = " + source.getClass().getSimpleName());
    };
  }

  default PolicyType toPolicyType(BasePolicyEntity source) {
    return toPolicyTypeSpecific(source, p -> TIME, p -> ROLE, p -> USER);
  }

  default Policy toPolicy(BasePolicyEntity source) {
    return toPolicyTypeSpecific(source, this::toTimePolicy, this::toRolePolicy, this::toUserPolicy);
  }

  default List<Policy> toPolicy(List<BasePolicyEntity> source) {
    if (isEmpty(source)) {
      return List.of();
    }
    return source.stream()
      .map(this::toPolicy).filter(Objects::nonNull).collect(toList());
  }

  default <T> T toPolicyTypeSpecific(BasePolicyEntity source, Function<TimePolicyEntity, T> mapperTimePolicy,
    Function<RolePolicyEntity, T> mapperRolePolicy, Function<UserPolicyEntity, T> mapperUserPolicy) {
    if (isNull(source)) {
      return null;
    }

    T result;
    if (source instanceof TimePolicyEntity p) {
      result = mapperTimePolicy.apply(p);
    } else if (source instanceof RolePolicyEntity p) {
      result = mapperRolePolicy.apply(p);
    } else if (source instanceof UserPolicyEntity p) {
      result = mapperUserPolicy.apply(p);
    } else {
      throw new UnsupportedOperationException("Mapper not defined: source type = " + source.getClass().getSimpleName());
    }

    return result;
  }
}
