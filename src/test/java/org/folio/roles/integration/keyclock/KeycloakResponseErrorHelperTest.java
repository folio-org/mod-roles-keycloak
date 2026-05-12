package org.folio.roles.integration.keyclock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakResponseErrorHelperTest {

  @Mock private Response response;

  @Test
  void errorDetails_positive_responseBodyIsPresent() {
    when(response.getStatusInfo()).thenReturn(Status.INTERNAL_SERVER_ERROR);
    when(response.hasEntity()).thenReturn(true);
    when(response.readEntity(String.class)).thenReturn("""
      {"error":"server_error", "error_description":"Permission rejected"}
      """);

    var result = KeycloakResponseErrorHelper.errorDetails(response);

    assertThat(result).isEqualTo("status = 500, message = Internal Server Error, "
      + "responseBody = {\"error\":\"server_error\", \"error_description\":\"Permission rejected\"}");
    verify(response).getStatusInfo();
    verify(response).hasEntity();
    verify(response).readEntity(String.class);
    verifyNoMoreInteractions(response);
  }

  @Test
  void errorDetails_positive_responseBodyIsAbsent() {
    when(response.getStatusInfo()).thenReturn(Status.UNAUTHORIZED);
    when(response.hasEntity()).thenReturn(false);

    var result = KeycloakResponseErrorHelper.errorDetails(response);

    assertThat(result).isEqualTo("status = 401, message = Unauthorized");
    verify(response).getStatusInfo();
    verify(response).hasEntity();
    verifyNoMoreInteractions(response);
  }

  @Test
  void errorDetails_positive_responseBodyIsBlank() {
    when(response.getStatusInfo()).thenReturn(Status.BAD_REQUEST);
    when(response.hasEntity()).thenReturn(true);
    when(response.readEntity(String.class)).thenReturn("  \n ");

    var result = KeycloakResponseErrorHelper.errorDetails(response);

    assertThat(result).isEqualTo("status = 400, message = Bad Request");
    verify(response).getStatusInfo();
    verify(response).hasEntity();
    verify(response).readEntity(String.class);
    verifyNoMoreInteractions(response);
  }

  @Test
  void errorDetails_positive_responseBodyReadFails() {
    when(response.getStatusInfo()).thenReturn(Status.INTERNAL_SERVER_ERROR);
    when(response.hasEntity()).thenReturn(true);
    when(response.readEntity(String.class)).thenThrow(new ProcessingException("stream closed"));

    var result = KeycloakResponseErrorHelper.errorDetails(response);

    assertThat(result).isEqualTo("status = 500, message = Internal Server Error, "
      + "responseBody = unavailable: ProcessingException: stream closed");
    verify(response).getStatusInfo();
    verify(response).hasEntity();
    verify(response).readEntity(String.class);
    verifyNoMoreInteractions(response);
  }
}
