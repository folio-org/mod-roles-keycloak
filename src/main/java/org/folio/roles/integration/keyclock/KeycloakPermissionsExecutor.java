package org.folio.roles.integration.keyclock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class KeycloakPermissionsExecutor {

  private final KeycloakConfigurationProperties keycloakConfigurationProperties;
  private final FolioExecutionContext folioExecutionContext;

  /**
   * Shared thread pool for parallel permission operations.
   * {@code null} when parallelism is configured to 1 (sequential mode).
   */
  private final ExecutorService executorService;

  public KeycloakPermissionsExecutor(
    KeycloakConfigurationProperties keycloakConfigurationProperties,
    FolioExecutionContext folioExecutionContext,
    @Qualifier("keycloakPermissionsExecutorService") @Autowired(required = false) ExecutorService executorService) {
    this.keycloakConfigurationProperties = keycloakConfigurationProperties;
    this.folioExecutionContext = folioExecutionContext;
    this.executorService = executorService;
  }

  public void execute(List<Endpoint> endpoints, Consumer<Endpoint> action) {
    if (endpoints == null || endpoints.isEmpty()) {
      return;
    }

    // Sequential execution: either no thread pool (parallelism <= 1) or only a single endpoint.
    // When there is a single endpoint, we skip thread dispatch to avoid unnecessary context-wrapping overhead.
    if (executorService == null || endpoints.size() == 1) {
      endpoints.forEach(action);
      return;
    }
    var batchSize = keycloakConfigurationProperties.getPermissions().getBatchSize();
    for (var batch : partition(endpoints, batchSize)) {
      executeBatch(batch, action, executorService);
    }
  }

  private void executeBatch(List<Endpoint> batch, Consumer<Endpoint> action, ExecutorService executor) {
    // executorService is guaranteed non-null here — this method is only called after the null-check in execute()
    var completionService = new ExecutorCompletionService<Void>(executor);
    var futures = new ArrayList<Future<Void>>(batch.size());

    for (var endpoint : batch) {
      futures.add(completionService.submit(wrapCallable(endpoint, action)));
    }

    for (int i = 0; i < batch.size(); i++) {
      try {
        completionService.take().get();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        cancelAll(futures);
        throw new IllegalStateException("Keycloak permissions execution interrupted", e);
      } catch (ExecutionException e) {
        cancelAll(futures);
        throw rethrow(e.getCause());
      }
    }
  }

  private Callable<Void> wrapCallable(Endpoint endpoint, Consumer<Endpoint> action) {
    // Capture the context eagerly on the calling thread, as FolioExecutionContext is ThreadLocal-based.
    // Calling getInstance() inside the worker thread would return a different (or null) context.
    var ctx = (FolioExecutionContext) folioExecutionContext.getInstance();
    return () -> {
      try (var ignored = new FolioExecutionContextSetter(ctx)) {
        action.accept(endpoint);
      }
      return null;
    };
  }

  private static void cancelAll(List<Future<Void>> futures) {
    futures.forEach(future -> future.cancel(true));
  }

  private static RuntimeException rethrow(Throwable error) {
    if (error instanceof Error e) {
      throw e;
    }
    if (error instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    return new IllegalStateException("Keycloak permissions execution failed", error);
  }

  private static <T> List<List<T>> partition(List<T> items, int batchSize) {
    var batches = new ArrayList<List<T>>();
    for (int i = 0; i < items.size(); i += batchSize) {
      batches.add(items.subList(i, Math.min(i + batchSize, items.size())));
    }
    return batches;
  }
}
