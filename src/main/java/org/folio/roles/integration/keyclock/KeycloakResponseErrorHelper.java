package org.folio.roles.integration.keyclock;

import static java.lang.String.format;

import jakarta.ws.rs.core.Response;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class KeycloakResponseErrorHelper {

  private static final int MAX_RESPONSE_BODY_LENGTH = 2048;

  static String errorDetails(Response response) {
    var statusInfo = response.getStatusInfo();
    var details = format("status = %s, message = %s", statusInfo.getStatusCode(), statusInfo.getReasonPhrase());
    var responseBody = responseBody(response);
    return responseBody == null ? details : details + ", responseBody = " + responseBody;
  }

  private static String responseBody(Response response) {
    try {
      if (!response.hasEntity()) {
        return null;
      }

      var responseBody = response.readEntity(String.class);
      return StringUtils.isBlank(responseBody)
        ? null
        : StringUtils.abbreviate(StringUtils.normalizeSpace(responseBody), MAX_RESPONSE_BODY_LENGTH);
    } catch (RuntimeException exception) {
      return format("unavailable: %s: %s", exception.getClass().getSimpleName(), exception.getMessage());
    }
  }
}
