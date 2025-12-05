package org.folio.roles.service.role;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.folio.roles.domain.dto.Role;

/**
 * Result of role creation operation with success and failure details.
 */
@Getter
public class RoleCreationResult {
  private final List<Role> successfulRoles;
  private final List<RoleCreationError> failures;

  public RoleCreationResult() {
    this.successfulRoles = new ArrayList<>();
    this.failures = new ArrayList<>();
  }

  public void addSuccess(Role role) {
    successfulRoles.add(role);
  }

  public void addFailure(String roleName, String errorMessage, Throwable cause) {
    failures.add(new RoleCreationError(roleName, errorMessage, cause));
  }

  public boolean hasFailures() {
    return !failures.isEmpty();
  }

  /**
   * Individual role creation failure details.
   */
  @Getter
  public static class RoleCreationError {
    private final String roleName;
    private final String errorMessage;
    private final String errorType;
    private final String rootCause;

    public RoleCreationError(String roleName, String errorMessage, Throwable cause) {
      this.roleName = roleName;
      this.errorMessage = errorMessage;
      this.errorType = cause != null ? cause.getClass().getSimpleName() : "Unknown";
      this.rootCause = extractRootCause(cause);
    }

    private String extractRootCause(Throwable throwable) {
      if (throwable == null) {
        return "No exception details available";
      }

      Throwable root = throwable;
      while (root.getCause() != null && root.getCause() != root) {
        root = root.getCause();
      }

      return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    public String getFullErrorMessage() {
      return String.format("%s [Type: %s, Root cause: %s]", errorMessage, errorType, rootCause);
    }
  }
}
