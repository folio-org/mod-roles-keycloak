package org.folio.roles.service.permission;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.roles.configuration.property.PermissionMappingProperties;
import org.folio.roles.exception.ServiceException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PermissionOverriderTest {

  @InjectMocks private PermissionOverrider permissionOverrider;
  @Mock private ObjectMapper objectMapper;
  @Mock private ResourceLoader resourceLoader;
  @Mock private PermissionMappingProperties permissionMappingProperties;

  @Test
  void getPermissionMappings_positive() throws IOException {
    var sourcePath = "sourcePath";
    var resource = mock(Resource.class);
    var inputStream = mock(InputStream.class);
    var permissions = new HashMap<String, PermissionData>();

    when(permissionMappingProperties.getSourcePath()).thenReturn(sourcePath);
    when(resourceLoader.getResource(sourcePath)).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(objectMapper.readValue(eq(inputStream), any(TypeReference.class))).thenReturn(permissions);

    permissionOverrider.getPermissionMappings();

    verify(permissionMappingProperties).getSourcePath();
    verify(resourceLoader).getResource(sourcePath);
    verify(resource).exists();
    verify(resource).getInputStream();
    verify(objectMapper).readValue(eq(inputStream), any(TypeReference.class));
  }

  @Test
  void getPermissionMappings_negative_resourceNotFound() {
    var sourcePath = "sourcePath";
    var resource = mock(Resource.class);

    when(permissionMappingProperties.getSourcePath()).thenReturn(sourcePath);
    when(resourceLoader.getResource(sourcePath)).thenReturn(resource);
    when(resource.exists()).thenReturn(false);

    assertThatThrownBy(() -> permissionOverrider.getPermissionMappings())
      .isInstanceOf(ServiceException.class)
      .hasMessage("Source is empty: " + sourcePath);

    verify(permissionMappingProperties).getSourcePath();
    verify(resourceLoader).getResource(sourcePath);
    verify(resource).exists();
  }

  @Test
  void getPermissionMappings_negative_failedToLoadResource() throws IOException {
    var sourcePath = "sourcePath";
    var resource = mock(Resource.class);
    var inputStream = mock(InputStream.class);

    when(permissionMappingProperties.getSourcePath()).thenReturn(sourcePath);
    when(resourceLoader.getResource(sourcePath)).thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(objectMapper.readValue(eq(inputStream), any(TypeReference.class))).thenThrow(JacksonException.class);

    assertThatThrownBy(() -> permissionOverrider.getPermissionMappings())
      .isInstanceOf(ServiceException.class)
      .hasMessage("Failed to load resource: " + sourcePath);

    verify(permissionMappingProperties).getSourcePath();
    verify(resourceLoader).getResource(sourcePath);
    verify(resource).exists();
    verify(resource).getInputStream();
    verify(objectMapper).readValue(eq(inputStream), any(TypeReference.class));
  }
}
