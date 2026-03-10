package org.folio.roles.integration.keyclock;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.HttpMethod;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.roles.support.TestUtils.TestModRolesKeycloakModuleMetadata;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@UnitTest
class KeycloakPermissionsExecutorTest {

  private ExecutorService sharedExecutorService;
  private KeycloakPermissionsExecutor executor;

  @AfterEach
  void tearDown() {
    if (sharedExecutorService != null) {
      sharedExecutorService.shutdownNow();
    }
  }

  @Test
  void executeRunsAllEndpoints() {
    executor = buildExecutor(2, 2);

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
  void executeRunsEndpointsSequentiallyWhenParallelismIsOne() {
    executor = buildExecutor(1, 10);

    var endpoints = List.of(
      new Endpoint().path("/a").method(HttpMethod.GET),
      new Endpoint().path("/b").method(HttpMethod.GET),
      new Endpoint().path("/c").method(HttpMethod.GET)
    );

    var seen = new ArrayList<Endpoint>();
    executor.execute(endpoints, seen::add);

    // Sequential path preserves insertion order
    assertThat(seen).containsExactlyElementsOf(endpoints);
  }

  @Test
  void executeSplitsEndpointsIntoBatches() {
    // 3 endpoints with batchSize=2 → 2 batches: [/a,/b] then [/c]
    executor = buildExecutor(2, 2);

    var endpoints = List.of(
      new Endpoint().path("/a").method(HttpMethod.GET),
      new Endpoint().path("/b").method(HttpMethod.GET),
      new Endpoint().path("/c").method(HttpMethod.GET)
    );

    var seen = new ConcurrentLinkedQueue<Endpoint>();
    executor.execute(endpoints, seen::add);

    // All 3 endpoints must be processed regardless of batch split
    assertThat(seen).hasSize(3).containsExactlyInAnyOrderElementsOf(endpoints);
  }

  @Test
  void executeDoesNothingForNullEndpoints() {
    executor = buildExecutor(2, 2);
    var seen = new ConcurrentLinkedQueue<Endpoint>();
    executor.execute(null, seen::add);
    assertThat(seen).isEmpty();
  }

  @Test
  void executeDoesNothingForEmptyEndpoints() {
    executor = buildExecutor(2, 2);
    var seen = new ConcurrentLinkedQueue<Endpoint>();
    executor.execute(List.of(), seen::add);
    assertThat(seen).isEmpty();
  }

  @Test
  void executeFailsFastOnError() {
    executor = buildExecutor(2, 2);

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
          if (!slowStarted.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("slow endpoint did not start within 5s");
          }
        } catch (InterruptedException interruptedException) {
          Thread.currentThread().interrupt();
        }
        throw new IllegalStateException("boom");
      }
    }));

    try {
      assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
        .isInstanceOf(ExecutionException.class)
        .hasCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("boom");

      boolean interrupted = false;
      try {
        interrupted = slowInterrupted.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      }
      assertThat(interrupted)
        .withFailMessage("slow endpoint thread was not interrupted within 5s")
        .isTrue();
    } finally {
      future.cancel(true);
      allowSlowExit.countDown();
      runner.shutdownNow();
    }
  }

  private KeycloakPermissionsExecutor buildExecutor(int parallelism, int batchSize) {
    var props = new KeycloakConfigurationProperties();
    var permissions = new KeycloakConfigurationProperties.Permissions();
    permissions.setParallelism(parallelism);
    permissions.setBatchSize(batchSize);
    props.setPermissions(permissions);
    var context = new DefaultFolioExecutionContext(new TestModRolesKeycloakModuleMetadata(), emptyMap());
    // Mirror the bean logic: allocate a fixed pool with core-thread timeout when parallelism > 1
    sharedExecutorService = parallelism > 1 ? buildExecutorService(parallelism) : null;
    return new KeycloakPermissionsExecutor(props, context, sharedExecutorService);
  }

  private static ExecutorService buildExecutorService(int parallelism) {
    var executor = new ThreadPoolExecutor(parallelism, parallelism, 60L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>());
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }
}
