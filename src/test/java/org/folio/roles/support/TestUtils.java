package org.folio.roles.support;

import static org.springframework.test.util.ReflectionTestUtils.getField;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.UUIDSerializer;
import com.esotericsoftware.kryo.util.Pool;
import java.util.Arrays;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  private static final Pool<Kryo> kryoPool;

  static {
    kryoPool = new Pool<>(true, false, 10) {
      protected Kryo create() {
        var kryo = new Kryo();

        kryo.register(UUID.class, new UUIDSerializer());
        kryo.setRegistrationRequired(false);

        return kryo;
      }
    };
  }

  public static void verifyNoMoreInteractions(Object testClassInstance) {
    var declaredFields = testClassInstance.getClass().getDeclaredFields();
    var mocks = Arrays.stream(declaredFields)
      .filter(field -> field.getAnnotation(Mock.class) != null || field.getAnnotation(Spy.class) != null)
      .map(field -> getField(testClassInstance, field.getName()))
      .toArray();

    Mockito.verifyNoMoreInteractions(mocks);
  }

  public static <T> T copy(T object) {
    Kryo kryo = kryoPool.obtain();

    var copy = kryo.copy(object);

    kryoPool.free(kryo);
    return copy;
  }
}
