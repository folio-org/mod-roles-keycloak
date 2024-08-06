package org.folio.roles.utils;

import static java.util.Arrays.stream;

import java.io.IOException;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component
public class ResourceHelper {
  private static final PathMatchingResourcePatternResolver PATH_RESOLVER =
    new PathMatchingResourcePatternResolver(ResourceHelper.class.getClassLoader());

  private final JsonHelper jsonHelper;

  /**
   * Loads json data from the specified directory and deserializes it to the specified type.
   *
   * @param resourceDir  directory to load json data from
   * @param resourceType type of data
   * @param <T>          type of data
   * @return list of deserialized data
   */
  public <T> Stream<T> readObjectsFromDirectory(String resourceDir, Class<T> resourceType) {
    return stream(getResources(resourceDir))
      .map(res -> deserializeResource(res, resourceType));
  }

  private <T> T deserializeResource(Resource res, Class<T> resourceType) {
    try {
      return jsonHelper.fromJsonStream(res.getInputStream(), resourceType);
    } catch (IOException e) {
      var msg =
        String.format("Failed to deserialize data of type %s from file: %s", resourceType, res.getFilename());
      throw new IllegalStateException(msg, e);
    }
  }

  private static Resource[] getResources(String resourceDir) {
    try {
      return PATH_RESOLVER.getResources(String.format("/%s/%s", resourceDir, "*.json"));
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load data for path: " + resourceDir, e);
    }
  }
}
