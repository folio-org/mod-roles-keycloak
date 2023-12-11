package org.folio.roles.support;

import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  public static void verifyNoMoreInteractions(Object testClassInstance) {
    var declaredFields = testClassInstance.getClass().getDeclaredFields();
    var mocks = Arrays.stream(declaredFields)
      .filter(field -> field.getAnnotation(Mock.class) != null || field.getAnnotation(Spy.class) != null)
      .map(field -> getField(testClassInstance, field.getName()))
      .toArray();

    Mockito.verifyNoMoreInteractions(mocks);
  }
}
