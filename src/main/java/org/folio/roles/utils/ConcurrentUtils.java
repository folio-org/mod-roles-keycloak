package org.folio.roles.utils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.roles.exception.ServiceException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConcurrentUtils {

  /**
   * Waits for all given futures to complete and collects every failure.
   *
   * <p>Unlike {@link CompletableFuture#allOf} this method does <em>not</em> short-circuit
   * on the first failure: it always waits for every future to finish so that no
   * error is silently lost when multiple futures fail concurrently.</p>
   *
   * <p>When at least one future fails the primary exception is thrown and every
   * additional exception is attached as a {@link Throwable#addSuppressed suppressed}
   * exception.</p>
   *
   * @param futures list of futures to join; must not be {@code null}
   * @throws RuntimeException   if one or more futures completed exceptionally and
   *                            the primary cause is a {@link RuntimeException}
   * @throws ServiceException   if the primary cause is a checked exception
   */
  public static void joinAll(List<CompletableFuture<Void>> futures) {
    // Wait for ALL futures to complete first, then inspect each one individually.
    // This ensures we capture every failure rather than only the first.
    try {
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    } catch (CompletionException ignored) {
      // Do not rethrow here; inspect each future individually to collect all failures.
    }
  
    throwIfAnyFailed(futures);
  }
  
  private static void throwIfAnyFailed(List<CompletableFuture<Void>> futures) {
    // Collect ALL failures so that no errors are silently lost when multiple futures fail.
    var exceptions = futures.stream()
      .filter(CompletableFuture::isCompletedExceptionally)
      .map(ConcurrentUtils::extractCause)
      .toList();
  
    if (exceptions.isEmpty()) {
      return;
    }
  
    var primary = exceptions.getFirst();
    exceptions.stream().skip(1).forEach(primary::addSuppressed);
    if (primary instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
    throw new ServiceException("Keycloak operation failed", primary);
  }
  
  private static Throwable extractCause(CompletableFuture<Void> f) {
    try {
      f.join();
      // Unreachable: isCompletedExceptionally guarantees join() throws.
      throw new IllegalStateException("Future was exceptionally completed but join() did not throw");
    } catch (CompletionException ex) {
      return ex.getCause() != null ? ex.getCause() : ex;
    }
  }
}
