package org.folio.roles.support;

import static org.springframework.test.util.ReflectionTestUtils.getField;
import static org.testcontainers.shaded.org.awaitility.Durations.FIVE_SECONDS;
import static org.testcontainers.shaded.org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.UUIDSerializer;
import com.esotericsoftware.kryo.util.Pool;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.awaitility.core.ThrowingRunnable;
import org.folio.spring.FolioModuleMetadata;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  private static final Pool<Kryo> KRYO_POOL;

  static {
    KRYO_POOL = new Pool<>(true, false, 10) {
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
    Kryo kryo = KRYO_POOL.obtain();

    try {
      return kryo.copy(object);
    } finally {
      KRYO_POOL.free(kryo);
    }
  }

  public static void awaitUntilAsserted(ThrowingRunnable throwingRunnable) {
    await().untilAsserted(throwingRunnable);
  }

  public static void awaitUntilAsserted(Duration initialDelay, ThrowingRunnable throwingRunnable) {
    await()
      .pollDelay(initialDelay)
      .untilAsserted(throwingRunnable);
  }

  public static ConditionFactory await() {
    return Awaitility.await()
      .atMost(FIVE_SECONDS)
      .pollInterval(ONE_HUNDRED_MILLISECONDS);
  }

  public static class TestModRolesKeycloakModuleMetadata implements FolioModuleMetadata {

    @Override
    public String getModuleName() {
      return "mod-roles-keycloak";
    }

    @Override
    public String getDBSchemaName(String tenantId) {
      return tenantId + "_mod_roles_keycloak";
    }
  }
}
