package org.folio.roles.integration.permissions;

import java.util.List;
import lombok.Data;

@Data
public class UserPermissions {

  private int totalRecords;
  private List<String> permissionNames;
}
