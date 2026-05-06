package org.folio.roles.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@Log4j2
@UnitTest
@ExtendWith(OutputCaptureExtension.class)
class LoggingContextConfigurationTest {

  @Test
  void logPattern_positive_printsFolioContextFields(CapturedOutput output) {
    var folioModuleMetadata = new FolioModuleMetadata() {
      @Override
      public String getModuleName() {
        return "mod-roles-keycloak";
      }

      @Override
      public String getDBSchemaName(String tenantId) {
        return tenantId;
      }
    };

    var folioExecutionContext = new FolioExecutionContext() {
      @Override
      public String getTenantId() {
        return "test-tenant";
      }

      @Override
      public String getRequestId() {
        return "test-request-id";
      }

      @Override
      public UUID getUserId() {
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
      }

      @Override
      public FolioModuleMetadata getFolioModuleMetadata() {
        return folioModuleMetadata;
      }
    };

    try (var ignored = new FolioExecutionContextSetter(folioExecutionContext)) {
      log.info("Logging context verification");
    }

    assertThat(output).contains("[test-request-id] [test-tenant] [00000000-0000-0000-0000-000000000001] "
      + "[mod-roles-keycloak] INFO");
  }
}
