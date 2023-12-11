package org.folio.roles.controller.validation.validator;

import static org.apache.commons.lang3.BooleanUtils.and;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.folio.roles.domain.dto.PolicyType.TIME;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import org.folio.roles.controller.validation.PolicyValidate;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.TimePolicy;

public class PolicyValidator implements ConstraintValidator<PolicyValidate, Policy> {

  @Override
  public boolean isValid(Policy policy, ConstraintValidatorContext context) {
    return isPolicyValid(policy, context);
  }

  private boolean isPolicyValid(Policy policy, ConstraintValidatorContext ctx) {
    var isValid = new AtomicBoolean(true);
    validateTimePolicy(policy, ctx, isValid);
    return isValid.get();
  }

  private void validateTimePolicy(Policy policy, ConstraintValidatorContext ctx, AtomicBoolean isValid) {
    if (TIME != policy.getType()) {
      return;
    }
    var timePolicy = policy.getTimePolicy();
    checkFirstDateBeforeSecond(timePolicy.getStart(), timePolicy.getExpires(), ctx, isValid,
      "expire date should be after start date");
    if (isTrue(timePolicy.getRepeat()) && isRequiredRepeatedDataPresented(timePolicy, ctx, isValid)) {
      checkFirstIntLessThanSecond(timePolicy.getMonthStart(), timePolicy.getMonthEnd(), ctx, isValid,
        "monthStart should be less than monthEnd");
      checkFirstIntLessThanSecond(timePolicy.getDayOfMonthStart(), timePolicy.getDayOfMonthEnd(), ctx, isValid,
        "dayOfMonthStart should be less than dayOfMonthEnd");
      checkFirstIntLessThanSecond(timePolicy.getHourStart(), timePolicy.getHourEnd(), ctx, isValid,
        "dayOfMonthStart should be less than dayOfMonthEnd");
      checkFirstIntLessThanSecond(timePolicy.getMinuteStart(), timePolicy.getMinuteEnd(), ctx, isValid,
        "minuteStart should be less than minuteEnd");
    }
  }

  private boolean isRequiredRepeatedDataPresented(TimePolicy timePolicy, ConstraintValidatorContext cxt,
                                                  AtomicBoolean isValid) {
    var allRequiredFields = new boolean[] {
      isNotEmptyField(timePolicy.getStart(), cxt, isValid, "start"),
      isNotEmptyField(timePolicy.getExpires(), cxt, isValid, "expires"),
      isNotEmptyField(timePolicy.getMonthStart(), cxt, isValid, "monthStart"),
      isNotEmptyField(timePolicy.getMonthEnd(), cxt, isValid, "monthEnd"),
      isNotEmptyField(timePolicy.getDayOfMonthStart(), cxt, isValid, "dayOfMonthStart"),
      isNotEmptyField(timePolicy.getDayOfMonthEnd(), cxt, isValid, "dayOfMonthEnd"),
      isNotEmptyField(timePolicy.getHourStart(), cxt, isValid, "hourStart"),
      isNotEmptyField(timePolicy.getHourEnd(), cxt, isValid, "hourEnd"),
      isNotEmptyField(timePolicy.getMinuteStart(), cxt, isValid, "minuteStart"),
      isNotEmptyField(timePolicy.getMinuteEnd(), cxt, isValid, "minuteEnd")};
    return and(allRequiredFields);
  }

  private static boolean addConstraintViolation(ConstraintValidatorContext context, String message,
                                                String propertyName) {
    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(message)
      .addPropertyNode(propertyName)
      .addConstraintViolation();
    return false;
  }

  private static boolean addConstraintViolation(ConstraintValidatorContext context, String message) {
    return addConstraintViolation(context, message, null);
  }

  private static boolean isNotEmptyField(Object obj, ConstraintValidatorContext context,
                                         AtomicBoolean validationContext, String propertyName) {
    if (isEmpty(obj)) {
      validationContext.set(false);
      return addConstraintViolation(context, "Property cannot be empty", propertyName);
    }
    return true;
  }

  private static void checkFirstDateBeforeSecond(Date first, Date second, ConstraintValidatorContext ctx,
                                                 AtomicBoolean isValid, String message) {
    if (second.after(first)) {
      return;
    }
    isValid.set(false);
    addConstraintViolation(ctx, message);
  }

  private static void checkFirstIntLessThanSecond(Integer first, Integer second, ConstraintValidatorContext ctx,
                                                  AtomicBoolean isValid, String message) {
    if (first < second) {
      return;
    }
    isValid.set(false);
    addConstraintViolation(ctx, message);
  }
}
