package org.folio.roles.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.folio.roles.exception.ServiceException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ConcurrentUtilsTest {

  @Nested
  @DisplayName("joinAll")
  class JoinAll {

    @Test
    void positive_allFuturesComplete() {
      var futures = List.of(
        CompletableFuture.<Void>completedFuture(null),
        CompletableFuture.<Void>completedFuture(null)
      );

      // Must not throw
      ConcurrentUtils.joinAll(futures);
    }

    @Test
    void positive_emptyFutureList() {
      // Must not throw
      ConcurrentUtils.joinAll(List.of());
    }

    @Test
    void negative_singleFailedFuture_runtimeException() {
      var cause = new IllegalStateException("task failed");
      var failed = CompletableFuture.<Void>failedFuture(cause);

      assertThatThrownBy(() -> ConcurrentUtils.joinAll(List.of(failed)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("task failed")
        .hasNoSuppressedExceptions();
    }

    @Test
    void negative_singleFailedFuture_entityNotFoundException() {
      var cause = new EntityNotFoundException("resource not found");
      var failed = CompletableFuture.<Void>failedFuture(cause);

      assertThatThrownBy(() -> ConcurrentUtils.joinAll(List.of(failed)))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("resource not found")
        .hasNoSuppressedExceptions();
    }

    @Test
    void negative_multipleFailedFutures_primaryThrownWithSuppressed() {
      var ex1 = new IllegalStateException("first failure");
      var ex2 = new IllegalArgumentException("second failure");
      var future1 = CompletableFuture.<Void>failedFuture(ex1);
      var future2 = CompletableFuture.<Void>failedFuture(ex2);

      assertThatThrownBy(() -> ConcurrentUtils.joinAll(List.of(future1, future2)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("first failure")
        .satisfies(ex -> assertThat(ex.getSuppressed())
          .hasSize(1)
          .allMatch(IllegalArgumentException.class::isInstance));
    }

    @Test
    void negative_checkedExceptionCause_wrappedInServiceException() {
      var checkedException = new Exception("checked failure");
      var failed = new CompletableFuture<Void>();
      failed.completeExceptionally(checkedException);

      assertThatThrownBy(() -> ConcurrentUtils.joinAll(List.of(failed)))
        .isInstanceOf(ServiceException.class)
        .hasMessage("Keycloak operation failed")
        .hasCause(checkedException);
    }

    @Test
    void positive_mixedSuccessAndFailure_failureThrown() {
      var cause = new RuntimeException("only one failed");
      var success = CompletableFuture.<Void>completedFuture(null);
      var failed = CompletableFuture.<Void>failedFuture(cause);

      assertThatThrownBy(() -> ConcurrentUtils.joinAll(List.of(success, failed)))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("only one failed")
        .hasNoSuppressedExceptions();
    }

    @Test
    void negative_allThreeFutures_primaryHasTwoSuppressed() {
      var ex1 = new RuntimeException("failure 1");
      var ex2 = new RuntimeException("failure 2");
      var ex3 = new RuntimeException("failure 3");

      var futures = List.of(
        CompletableFuture.<Void>failedFuture(ex1),
        CompletableFuture.<Void>failedFuture(ex2),
        CompletableFuture.<Void>failedFuture(ex3)
      );

      assertThatThrownBy(() -> ConcurrentUtils.joinAll(futures))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("failure 1")
        .satisfies(ex -> assertThat(ex.getSuppressed()).hasSize(2));
    }
  }
}
