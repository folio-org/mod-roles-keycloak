package org.folio.roles.integration.keyclock;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakPermissionsExecutorTest {

  @Test
  void executeRunsAllEndpoints() {
    var props = new KeycloakConfigurationProperties();
    var permissions = new KeycloakConfigurationProperties.Permissions();
    permissions.setParallelism(2);
    permissions.setBatchSize(2);
    props.setPermissions(permissions);

    var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    var executor = new KeycloakPermissionsExecutor(props, context);

    var endpoints = List.of(
      new Endpoint().path("/a").method(HttpMethod.GET),
      new Endpoint().path("/b").method(HttpMethod.GET),
      new Endpoint().path("/c").method(HttpMethod.GET)
    );

    var seen = new ConcurrentLinkedQueue<Endpoint>();
    executor.execute(endpoints, seen::add);

    assertThat(seen).containsExactlyInAnyOrderElementsOf(endpoints);
  }

  @Test
  void executeFailsFastOnError() {
    var props = new KeycloakConfigurationProperties();
    var permissions = new KeycloakConfigurationProperties.Permissions();
    permissions.setParallelism(2);
    permissions.setBatchSize(3);
    props.setPermissions(permissions);

    var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    var executor = new KeycloakPermissionsExecutor(props, context);

    var endpoints = List.of(
      new Endpoint().path("/a").method(HttpMethod.GET),
      new Endpoint().path("/b").method(HttpMethod.GET)
    );

    var counter = new AtomicInteger(0);
    assertThatThrownBy(() -> executor.execute(endpoints, endpoint -> {
      if (endpoint.getPath().equals("/b")) {
        throw new IllegalStateException("boom");
      }
      counter.incrementAndGet();
    })).isInstanceOf(IllegalStateException.class);

    assertThat(counter.get()).isGreaterThanOrEqualTo(0);
  }
}
