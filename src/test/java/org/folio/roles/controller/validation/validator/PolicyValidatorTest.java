package org.folio.roles.controller.validation.validator;

import static org.folio.roles.domain.dto.PolicyType.TIME;
import static org.folio.roles.support.PolicyUtils.createTimePolicy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.util.Date;
import org.folio.roles.domain.dto.Policy;
import org.folio.roles.domain.dto.TimePolicy;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PolicyValidatorTest {

  @Mock
  private ConstraintValidatorContext context;
  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder builder;
  @Mock
  private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;
  @InjectMocks
  private PolicyValidator validator;

  @Test
  void negative_isNotValid() {
    var timePolicy = new TimePolicy();
    timePolicy.setStart(new Date(System.currentTimeMillis() + 100000));
    timePolicy.setExpires(new Date());
    var policy = new Policy();
    policy.setType(TIME);
    policy.setTimePolicy(timePolicy);

    when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
    when(builder.addPropertyNode(any())).thenReturn(nodeBuilder);

    boolean isValid = validator.isValid(policy, context);

    assertFalse(isValid);
    verify(context).buildConstraintViolationWithTemplate("expire date should be after start date");
  }

  @Test
  void positive_notRepeatedTimePolicyIsValid() {
    var timePolicy = new TimePolicy();
    timePolicy.setStart(new Date());
    timePolicy.setExpires(new Date(System.currentTimeMillis() + 100000));
    var policy = new Policy();
    policy.setType(TIME);
    policy.setTimePolicy(timePolicy);

    boolean isValid = validator.isValid(policy, context);

    assertTrue(isValid);
    verifyNoInteractions(context);
  }

  @Test
  void positive_repeatedTimePolicyIsValid() {
    var policy = createTimePolicy();

    boolean isValid = validator.isValid(policy, context);

    assertTrue(isValid);
    verifyNoInteractions(context);
  }
}
