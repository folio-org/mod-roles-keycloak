package org.folio.roles.integration.keyclock.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties.Concurrency;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakConfigurationTest {

  @InjectMocks private KeycloakConfiguration keycloakConfiguration;
  @Mock private KeycloakConfigurationProperties configuration;

  private ExecutorService createdExecutor;

  @AfterEach
  void tearDown() {
    if (createdExecutor != null) {
      createdExecutor.shutdownNow();
    }
  }

  @Nested
  @DisplayName("keycloakOperationsExecutor")
  class KeycloakOperationsExecutor {

    @Test
    @DisplayName("positive - returns a non-null ExecutorService")
    void positive_returnsNonNull() {
      var concurrency = new Concurrency();
      concurrency.setThreadPoolSize(5);
      when(configuration.getConcurrency()).thenReturn(concurrency);

      createdExecutor = keycloakConfiguration.keycloakOperationsExecutor();

      assertThat(createdExecutor).isNotNull();
    }

    @Test
    @DisplayName("positive - executor runs tasks on virtual threads named keycloak-op-N")
    void positive_tasksRunOnVirtualThreadsWithExpectedName() throws InterruptedException {
      var concurrency = new Concurrency();
      concurrency.setThreadPoolSize(4);
      when(configuration.getConcurrency()).thenReturn(concurrency);

      createdExecutor = keycloakConfiguration.keycloakOperationsExecutor();

      var latch = new CountDownLatch(1);
      var capturedThread = new AtomicReference<Thread>();

      createdExecutor.execute(() -> {
        capturedThread.set(Thread.currentThread());
        latch.countDown();
      });

      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

      var thread = capturedThread.get();
      assertThat(thread.isVirtual())
        .as("executor must use virtual threads")
        .isTrue();
      assertThat(thread.getName())
        .as("thread name must start with 'keycloak-op-'")
        .startsWith("keycloak-op-");
    }

    @Test
    @DisplayName("positive - executor respects max-concurrency cap: queues tasks beyond pool size")
    void positive_queuedTasksAreNotRejected() throws InterruptedException {
      // Pool size = 2; submit 6 tasks. If SynchronousQueue were used the 3rd would be
      // rejected; with a queuing executor all 6 must complete without exception.
      var concurrency = new Concurrency();
      concurrency.setThreadPoolSize(2);
      when(configuration.getConcurrency()).thenReturn(concurrency);

      createdExecutor = keycloakConfiguration.keycloakOperationsExecutor();

      int taskCount = 6;
      var latch = new CountDownLatch(taskCount);

      for (int i = 0; i < taskCount; i++) {
        // Must not throw RejectedExecutionException
        createdExecutor.execute(latch::countDown);
      }

      assertThat(latch.await(10, TimeUnit.SECONDS))
        .as("all %d tasks must complete without being rejected", taskCount)
        .isTrue();
    }

    @Test
    @DisplayName("positive - executor shuts down gracefully via destroyMethod")
    void positive_shutdownCompletesCleanly() throws InterruptedException {
      var concurrency = new Concurrency();
      concurrency.setThreadPoolSize(2);
      when(configuration.getConcurrency()).thenReturn(concurrency);

      var executor = keycloakConfiguration.keycloakOperationsExecutor();
      createdExecutor = executor; // also cleaned up by @AfterEach

      executor.shutdown();

      assertThat(executor.awaitTermination(5, TimeUnit.SECONDS))
        .as("executor must terminate within 5 seconds after shutdown()")
        .isTrue();
      assertThat(executor.isShutdown()).isTrue();
    }
  }
}
