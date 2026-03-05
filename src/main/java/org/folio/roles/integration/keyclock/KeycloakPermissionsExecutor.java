package org.folio.roles.integration.keyclock;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakPermissionsExecutor {

  private final KeycloakConfigurationProperties keycloakConfigurationProperties;
  private final FolioExecutionContext folioExecutionContext;

  private volatile ExecutorService executor;

  public void execute(List<Endpoint> endpoints, Consumer<Endpoint> action) {
    if (endpoints == null || endpoints.isEmpty()) {
      return;
    }

    var permissionsConfig = keycloakConfigurationProperties.getPermissions();
    var parallelism = permissionsConfig.getParallelism();
    if (parallelism <= 1 || endpoints.size() <= 1) {
      endpoints.forEach(action);
      return;
    }

    var batchSize = permissionsConfig.getBatchSize();
    for (var batch : partition(endpoints, batchSize)) {
      executeBatch(batch, action, parallelism);
    }
  }

  private void executeBatch(List<Endpoint> batch, Consumer<Endpoint> action, int parallelism) {
    var completionService = new ExecutorCompletionService<Void>(getOrCreateExecutor(parallelism));
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
    return () -> {
      var ctx = (FolioExecutionContext) folioExecutionContext.getInstance();
      try (var ignored = new FolioExecutionContextSetter(ctx)) {
        action.accept(endpoint);
      }
      return null;
    };
  }

  private ExecutorService getOrCreateExecutor(int parallelism) {
    if (executor == null) {
      synchronized (this) {
        if (executor == null) {
          executor = Executors.newFixedThreadPool(parallelism);
        }
      }
    }
    return executor;
  }

  private static void cancelAll(List<Future<Void>> futures) {
    futures.forEach(future -> future.cancel(true));
  }

  private static RuntimeException rethrow(Throwable error) {
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

  @PreDestroy
  public void shutdown() {
    if (executor != null) {
      executor.shutdown();
    }
  }
}
