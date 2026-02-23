package org.folio.roles.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakTransactionHelperTest {

  private MockedStatic<TransactionSynchronizationManager> txManagerMock;

  @BeforeEach
  void setUp() {
    txManagerMock = mockStatic(TransactionSynchronizationManager.class);
  }

  @AfterEach
  void tearDown() {
    txManagerMock.close();
  }

  @Nested
  @DisplayName("executeWithCompensation - no active transaction")
  class NoActiveTransaction {

    @Test
    void positive_withReturnValue() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
      when(databaseAction.get()).thenReturn("success");

      var result = KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
          compensationAction);

      assertThat(result).isEqualTo("success");
      verify(keycloakAction).run();
      verify(databaseAction).get();
      txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()), never());
      verifyNoInteractions(compensationAction);
    }

    @Test
    void positive_withoutReturnValue() {
      var keycloakAction = mock(Runnable.class);
      var databaseAction = mock(Runnable.class);
      var compensationAction = mock(Runnable.class);

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);

      KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

      verify(keycloakAction).run();
      verify(databaseAction).run();
      verifyNoInteractions(compensationAction);
    }

    @Test
    void negative_dbFails_compensationSucceeds() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      var dbException = new RuntimeException("DB failed");
      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
      when(databaseAction.get()).thenThrow(dbException);

      assertThatThrownBy(() -> KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
          compensationAction))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("DB failed");

      verify(keycloakAction).run();
      verify(databaseAction).get();
      verify(compensationAction).run();
    }

    @Test
    void negative_runnableDbFails_compensationSucceeds() {
      var keycloakAction = mock(Runnable.class);
      var databaseAction = mock(Runnable.class);
      var compensationAction = mock(Runnable.class);

      var dbException = new RuntimeException("Runnable DB failed");
      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
      doThrow(dbException).when(databaseAction).run();

      assertThatThrownBy(() -> KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
          compensationAction))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Runnable DB failed");

      verify(keycloakAction).run();
      verify(databaseAction).run();
      verify(compensationAction).run();
    }

    @Test
    void negative_dbFails_compensationAlsoFails_exceptionSuppressed() {
      var keycloakAction = mock(Runnable.class);
      var databaseAction = mock(Runnable.class);
      var compensationAction = mock(Runnable.class);

      var dbException = new RuntimeException("DB failed");
      var compException = new RuntimeException("Compensation failed");

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
      doThrow(dbException).when(databaseAction).run();
      doThrow(compException).when(compensationAction).run();

      assertThatThrownBy(() -> KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
          compensationAction))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("DB failed")
          .satisfies(ex -> assertThat(ex.getSuppressed()).containsExactly(compException));

      verify(keycloakAction).run();
      verify(databaseAction).run();
      verify(compensationAction).run();
    }

    @Test
    void negative_dbThrowsCheckedException_wrappedInRuntimeException() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      var checkedException = new Exception("Checked DB failure");
      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(false);
      doAnswer(invocation -> {
        throw checkedException;
      }).when(databaseAction).get();

      assertThatThrownBy(() -> KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
          compensationAction))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("Database action failed")
          .hasCause(checkedException);

      verify(keycloakAction).run();
      verify(databaseAction).get();
      verify(compensationAction).run();
    }
  }

  @Nested
  @DisplayName("executeWithCompensation - active transaction")
  class ActiveTransaction {

    @Test
    void positive_synchronizationRegistered() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
      when(databaseAction.get()).thenReturn("success");

      var result = KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
          compensationAction);

      assertThat(result).isEqualTo("success");
      verify(keycloakAction).run();
      verify(databaseAction).get();
      txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(any()));
      verifyNoInteractions(compensationAction);
    }

    @Test
    void positive_rollbackExecutesCompensation() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
      when(databaseAction.get()).thenReturn("success");

      KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

      var syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
      txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

      syncCaptor.getValue().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

      verify(compensationAction).run();
    }

    @Test
    void negative_rollbackCompensationFails_exceptionSwallowed() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
      when(databaseAction.get()).thenReturn("success");
      doThrow(new RuntimeException("Compensation failed during rollback")).when(compensationAction).run();

      KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

      var syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
      txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

      // Exception is caught and logged inside afterCompletion: must NOT propagate
      syncCaptor.getValue().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

      verify(compensationAction).run();
    }

    @Test
    void positive_commitIgnoresCompensation() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
      when(databaseAction.get()).thenReturn("success");

      KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

      var syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
      txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

      syncCaptor.getValue().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

      verifyNoInteractions(compensationAction);
    }

    @Test
    @DisplayName("STATUS_UNKNOWN: compensation is NOT run and no exception propagates")
    void positive_statusUnknown_doesNotRunCompensation() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
      when(databaseAction.get()).thenReturn("success");

      KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction, compensationAction);

      var syncCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
      txManagerMock.verify(() -> TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));

      // Should NOT throw and should NOT call compensation: outcome is indeterminate,
      // so we log at ERROR level and do nothing to avoid creating a new
      // inconsistency.
      syncCaptor.getValue().afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

      verifyNoInteractions(compensationAction);
    }

    @Test
    void negative_dbFails_compensationDeferredToSpringRollback() {
      var keycloakAction = mock(Runnable.class);
      @SuppressWarnings("unchecked")
      var databaseAction = (Supplier<String>) mock(Supplier.class);
      var compensationAction = mock(Runnable.class);

      var dbException = new RuntimeException("DB failed");
      txManagerMock.when(TransactionSynchronizationManager::isSynchronizationActive).thenReturn(true);
      when(databaseAction.get()).thenThrow(dbException);

      assertThatThrownBy(() -> KeycloakTransactionHelper.executeWithCompensation(keycloakAction, databaseAction,
          compensationAction))
          .isInstanceOf(RuntimeException.class)
          .hasMessage("DB failed");

      // Compensation is NOT triggered inline; deferred to Spring's
      // afterCompletion(STATUS_ROLLED_BACK)
      verify(compensationAction, never()).run();
    }
  }
}
