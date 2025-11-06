package org.folio.roles.migration;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.ValidationErrors;
import liquibase.integration.spring.SpringResourceAccessor;
import liquibase.resource.ResourceAccessor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.context.ApplicationContext;

/**
 * Abstract base class for Liquibase custom task migrations that need access to Spring beans.
 *
 * <p>This class provides access to Spring ApplicationContext through reflection,
 * allowing custom migrations to use Spring-managed services.</p>
 *
 * <p>Based on the pattern from mod-scheduler.</p>
 */
@Log4j2
public abstract class AbstractCustomTaskChangeMigration implements CustomTaskChange {

  protected ApplicationContext springApplicationContext;

  @Override
  public String getConfirmationMessage() {
    return "Completed " + this.getClass().getSimpleName();
  }

  @Override
  public void setUp() {
    // Do nothing
  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {
    try {
      var springResourceAccessor = (SpringResourceAccessor) resourceAccessor;
      springApplicationContext =
        (ApplicationContext) FieldUtils.readField(springResourceAccessor, "resourceLoader", true);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Failed to obtain Spring Application Context", e);
    }
  }

  @Override
  public ValidationErrors validate(Database database) {
    return null;
  }
}
