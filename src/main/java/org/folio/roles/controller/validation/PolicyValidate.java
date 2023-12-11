package org.folio.roles.controller.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.roles.controller.validation.validator.PolicyValidator;

@Documented
@Constraint(validatedBy = PolicyValidator.class)
@Target({TYPE_USE, METHOD, FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PolicyValidate {

  String message() default "Not correct request body";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
