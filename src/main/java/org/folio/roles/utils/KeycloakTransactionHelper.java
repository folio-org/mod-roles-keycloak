package org.folio.roles.utils;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeycloakTransactionHelper {

  /**
   * Executes a Keycloak action, then a DB action. If the DB action fails,
   * executes a compensating Keycloak action.
   *
   * <p>
   * When called inside an active Spring transaction, the compensation action is
   * registered as a
   * {@link TransactionSynchronization} and will be triggered by Spring on
   * {@link TransactionSynchronization#STATUS_ROLLED_BACK}. The synchronization is
   * registered <em>after</em>
   * {@code databaseAction} succeeds, so it will only fire if the enclosing
   * transaction rolls back due to a
   * <em>subsequent</em> failure — not due to a failure originating inside this
   * method.
   *
   * <p>
   * <strong>Note:</strong> Nesting {@code executeWithCompensation} calls inside
   * the same transaction will register
   * multiple synchronizations. Each will independently compensate on rollback.
   * Callers should ensure the resulting
   * combined compensation sequence is safe and idempotent.
   *
   * @param keycloakAction     The primary Keycloak action (e.g., create
   *                           permissions)
   * @param databaseAction     The primary database action (e.g., save entities)
   *                           returning a value
   * @param compensationAction The compensating Keycloak action (e.g., delete
   *                           permissions) to run if DB fails or
   *                           transaction rolls back
   * @param <T>                The return type of the database action
   * @return The result of the database action
   */
  public static <T> T executeWithCompensation(
      Runnable keycloakAction,
      Supplier<T> databaseAction,
      Runnable compensationAction) {

    keycloakAction.run();

    try {
      T result = databaseAction.get();

      // Register compensation AFTER the DB action succeeds.
      // This guarantees the synchronization is only triggered if a *subsequent*
      // operation in the same transaction causes a rollback, not by this DB action
      // itself.
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager
            .registerSynchronization(new CompensationSynchronization(compensationAction));
      }

      return result;
    } catch (Exception dbException) {
      handleFallbackCompensation(dbException, compensationAction);
      if (dbException instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Database action failed in KeycloakTransactionHelper", dbException);
    }
  }

  /**
   * Executes a Keycloak action, then a DB action without a return value.
   *
   * @param keycloakAction     The primary Keycloak action (e.g., create
   *                           permissions)
   * @param databaseAction     The primary database action (e.g., delete entities)
   * @param compensationAction The compensating Keycloak action (e.g., create
   *                           permissions) to run if DB fails or
   *                           transaction rolls back
   */
  public static void executeWithCompensation(
      Runnable keycloakAction,
      Runnable databaseAction,
      Runnable compensationAction) {
    executeWithCompensation(keycloakAction, () -> {
      databaseAction.run();
      return null;
    }, compensationAction);
  }

  private static void handleFallbackCompensation(Exception dbException, Runnable compensationAction) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      // The DB action failed inside an active Spring-managed transaction.
      // Spring will call afterCompletion(STATUS_ROLLED_BACK) on the registered
      // CompensationSynchronization, which will execute the compensation action.
      // Nothing to do here — compensation is deferred to the transaction callback.
      log.debug("Database action failed inside an active transaction; "
          + "Keycloak compensation is deferred to Spring rollback callback.", dbException);
    } else {
      log.warn("Database operation failed outside of an active transaction. "
          + "Attempting to execute Keycloak compensation action.", dbException);
      try {
        compensationAction.run();
      } catch (Exception compensationException) {
        log.error("CRITICAL: Keycloak compensation action failed after database operation failure! "
            + "System may be in an inconsistent state.", compensationException);
        dbException.addSuppressed(compensationException);
      }
    }
  }

  private static class CompensationSynchronization implements TransactionSynchronization {
    private final Runnable compensationAction;

    CompensationSynchronization(Runnable compensationAction) {
      this.compensationAction = compensationAction;
    }

    @Override
    public void afterCompletion(int status) {
      if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
        try {
          log.warn("Transaction rolled back. Executing Keycloak compensation action.");
          compensationAction.run();
        } catch (Exception compensationException) {
          log.error("CRITICAL: Keycloak compensation action failed "
              + "during transaction rollback! System may be in an inconsistent state.",
              compensationException);
        }
      } else if (status == TransactionSynchronization.STATUS_UNKNOWN) {
        // Transaction outcome is indeterminate (e.g., connection reset during commit).
        // The Keycloak action may or may not have been committed; the DB state is
        // unknown.
        // Compensation is intentionally NOT run here to avoid creating a new
        // inconsistency;
        // instead we log at error level so on-call teams can investigate and reconcile
        // manually.
        log.error("CRITICAL: Transaction completed with UNKNOWN status. "
            + "Keycloak and database states may be inconsistent. "
            + "Manual reconciliation may be required.");
      }
    }
  }
}
