package org.folio.roles;

import org.folio.common.configuration.properties.FolioEnvironment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
@Import(FolioEnvironment.class)
public class RolesApplication {

  /**
   * Runs spring application.
   *
   * @param args command line arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(RolesApplication.class, args);
  }
}
