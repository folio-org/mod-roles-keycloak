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
   * When called inside an active Spring transaction, a {@link TransactionSynchronization}
   * is registered so that compensation fires on
   * {@link TransactionSynchronization#STATUS_ROLLED_BACK}. The synchronization is
   * registered regardless of whether {@code databaseAction} succeeds or fails,
   * because the Keycloak side-effect has already occurred and must be reversed
   * whenever the enclosing transaction does not commit.
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

    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager
        .registerSynchronization(new CompensationSynchronization(compensationAction));
    }

    try {
      return databaseAction.get();
    } catch (Exception dbException) {
      handleFallbackCompensation(dbException, compensationAction);
      throw (RuntimeException) dbException;
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

  private record CompensationSynchronization(Runnable compensationAction) implements TransactionSynchronization {

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
        log.error("CRITICAL: Transaction completed with UNKNOWN status. "
          + "Keycloak and database states may be inconsistent. "
          + "Manual reconciliation may be required.");
      }
    }
  }
}
