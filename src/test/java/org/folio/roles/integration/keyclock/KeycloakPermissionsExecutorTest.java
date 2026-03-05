package org.folio.roles.integration.keyclock;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
    permissions.setBatchSize(2);
    props.setPermissions(permissions);

    var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    var executor = new KeycloakPermissionsExecutor(props, context);

    var endpoints = List.of(
      new Endpoint().path("/slow").method(HttpMethod.GET),
      new Endpoint().path("/fail").method(HttpMethod.GET)
    );

    var slowStarted = new CountDownLatch(1);
    var allowSlowExit = new CountDownLatch(1);
    var slowInterrupted = new CountDownLatch(1);
    ExecutorService runner = Executors.newSingleThreadExecutor();
    Future<?> future = runner.submit(() -> executor.execute(endpoints, endpoint -> {
      if (endpoint.getPath().equals("/slow")) {
        slowStarted.countDown();
        try {
          allowSlowExit.await();
        } catch (InterruptedException interruptedException) {
          slowInterrupted.countDown();
          Thread.currentThread().interrupt();
        }
        return;
      }

      if (endpoint.getPath().equals("/fail")) {
        try {
          if (!slowStarted.await(1, TimeUnit.SECONDS)) {
            throw new IllegalStateException("slow endpoint did not start within 1s");
          }
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
        }
        throw new IllegalStateException("boom");
      }
    }));

    try {
      assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("boom");

      boolean interrupted = false;
      try {
        interrupted = slowInterrupted.await(1, TimeUnit.SECONDS);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      }
      assertThat(interrupted)
        .withFailMessage("slow endpoint thread was not interrupted within 1s")
        .isTrue();
    } finally {
      future.cancel(true);
      allowSlowExit.countDown();
      runner.shutdownNow();
    }
  }
}
